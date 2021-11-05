package com.laytonsmith.core.constructs;

import com.laytonsmith.PureUtilities.Common.ArrayUtils;
import com.laytonsmith.PureUtilities.Pair;
import com.laytonsmith.PureUtilities.Version;
import com.laytonsmith.annotations.typeof;
import com.laytonsmith.core.FullyQualifiedClassName;
import com.laytonsmith.core.MSVersion;
import com.laytonsmith.core.compiler.CompilerEnvironment;
import com.laytonsmith.core.constructs.generics.GenericDeclaration;
import com.laytonsmith.core.constructs.generics.GenericParameters;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.exceptions.CRE.CRECastException;
import com.laytonsmith.core.exceptions.CRE.CREUnsupportedOperationException;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.natives.interfaces.MEnumType;
import com.laytonsmith.core.natives.interfaces.Mixed;
import com.laytonsmith.core.objects.ObjectDefinition;
import com.laytonsmith.core.objects.ObjectDefinitionNotFoundException;
import com.laytonsmith.core.objects.ObjectDefinitionTable;
import com.laytonsmith.core.objects.UserObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A CClassType represents a reference to a MethodScript class.
 */
@typeof("ms.lang.ClassType")
@SuppressWarnings("checkstyle:overloadmethodsdeclarationorder")
public final class CClassType extends Construct implements com.laytonsmith.core.natives.interfaces.Iterable {

	public static final String PATH_SEPARATOR = FullyQualifiedClassName.PATH_SEPARATOR;

	private static final ClassTypeCache CACHE = new ClassTypeCache();

	// The only types that can be created here are the ones that don't have a real class associated with them, or the
	// TYPE value itself
	@SuppressWarnings("FieldNameHidesFieldInSuperclass")
	public static final CClassType TYPE;
	public static final CClassType AUTO;
	/**
	 * Used to differentiate between null and uninitialized.
	 *
	 * NOTE: This must come before the below static blocks are run.
	 */
	private static final Mixed[] UNINITIALIZED = new Mixed[0];

	static {
		try {
			TYPE = new CClassType("ms.lang.ClassType", Target.UNKNOWN, null);
			AUTO = new CClassType("auto", Target.UNKNOWN, null);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
	}

	/**
	 * This should generally be used instead of creating a new empty array in getInterfaces, if no interfaces are
	 * implemented by this class. This saves memory.
	 */
	public static final CClassType[] EMPTY_CLASS_ARRAY = new CClassType[0];

	static {
		CACHE.add(FullyQualifiedClassName.forNativeClass(CClassType.class), null , TYPE);
	}

	private final boolean isTypeUnion;
	private final FullyQualifiedClassName fqcn;
	private final GenericParameters genericParameters;

	/**
	 * This is an invalid instance of the underlying type that can only be used for Documentation purposes or finding
	 * out meta information about the class. Because these can be a type union, this is an array.
	 *
	 * DO NOT USE THIS VALUE WITHOUT FIRST CALLING {@link #instantiateInvalidType}
	 */
	private Mixed[] invalidType = UNINITIALIZED;

	/**
	 * If this was constructed against a native class, we can do some optimizations in the course
	 * of operation. This may be null, and all code in this class must support the mechanisms if this
	 * is null anyways, but if it isn't null, then this can perhaps be used to help optimize.
	 */
	private Class<? extends Mixed> nativeClass = null;

	/**
	 * This *MUST* contain a list of non type union types.
	 */
	private final SortedSet<FullyQualifiedClassName> types = new TreeSet<>();

	private final GenericDeclaration genericDeclaration;

	/**
	 * Returns the singular instance of CClassType that represents this type.
	 *
	 * <p>IMPORTANT: The type MUST be fully qualified AND exist as a real, instantiable class, or this will cause
	 * errors. The only time this method is preferred vs {@link #get(com.laytonsmith.core.FullyQualifiedClassName)} is
	 * when used to define the TYPE value. The native class must also be provided at the same time, which is used
	 * for various operations to increase efficiency when dealing with native classes. If the type is defined with
	 * generics, use {@link #getWithGenericDeclaration(Class, GenericDeclaration)}.
	 *
	 * Unlike the other getters, this will not throw a ClassNotFoundException, it will instead throw an Error.
	 * @param type
	 * @return
	 */
	public static CClassType get(Class<? extends Mixed> type) {
		return getWithGenericDeclaration(type, null);
	}

	/**
	 * Returns the singular instance of CClassType that represents this type.
	 *
	 * <p>IMPORTANT: The type MUST be fully qualified AND exist as a real, instantiable class, or this will cause
	 * errors. The only time this method is preferred vs {@link #get(com.laytonsmith.core.FullyQualifiedClassName)} is
	 * when used to define the TYPE value. The native class must also be provided at the same time, which is used
	 * for various operations to increase efficiency when dealing with native classes.
	 *
	 * Unlike the other getters, this will not throw a ClassNotFoundException, it will instead throw an Error.
	 * @param type
	 * @return
	 */
	public static CClassType getWithGenericDeclaration(Class<? extends Mixed> type, GenericDeclaration generics) {
		FullyQualifiedClassName fqcn = FullyQualifiedClassName.forNativeClass(type);
		CClassType classtype = getNakedClassType(fqcn);
		if(classtype == null) {
			// hasn't been defined yet
			classtype = defineClass(fqcn, generics);
			classtype.nativeClass = type;
		}
		return classtype;
	}

	/**
	 * Returns the "naked class type". This is the type without any parameters defined. In general, this represents
	 * a non-instantiatable class, but can be used in certain circumstances, particularly when the compiler needs to
	 * verify the generic declaration.
	 * @param type
	 * @return
	 */
	public static CClassType getNakedClassType(FullyQualifiedClassName type) {
		return CACHE.getNakedClassType(type);
	}

	/**
	 * Returns the "naked class type". This is the type without any parameters defined. In general, this represents
	 * a non-instantiatable class, but can be used in certain circumstances, particularly when the compiler needs to
	 * verify the generic declaration or for very generic instanceof checks.
	 *
	 * @return
	 */
	public CClassType getNakedType() {
		return CClassType.getNakedClassType(this.getFQCN());
	}

	/**
	 * Returns the singular instance of CClassType that represents this type. If it doesn't exist, it creates it, stores,
	 * and returns that instance. Note that in general, == is not supported for these types. This method will only
	 * succeed on types that don't have a generic declaration, for ones with, you must use
	 * {@link #get(FullyQualifiedClassName, Target, GenericParameters)}
	 *
	 * @param type
	 * @return
	 */
	public static CClassType get(FullyQualifiedClassName type)
			throws ClassNotFoundException {
		return get(type, Target.UNKNOWN, null);
	}

	/**
	 * Returns the singular instance of CClassType that represents this type. If it doesn't exist, it creates it, stores,
	 * and returns that instance. Note that in general, == is not supported for these types.
	 *
	 * @param type
	 * @return
	 */
	public static CClassType get(FullyQualifiedClassName type, Target t, GenericParameters generics)
			throws ClassNotFoundException {
		assert type != null;
		CClassType naked = getNakedClassType(type);
		if(naked == null && generics != null) {
			throw new ClassNotFoundException("Naked class for " + type.getFQCN()
					+ " is not yet defined, it must be defined before use.");
		}
		if(naked.getGenericDeclaration() != null && generics == null) {
			throw new ClassNotFoundException("Missing generic parameters for " + type.getFQCN());
		}
		CClassType ctype = CACHE.get(type, generics);
		if(ctype == null) {
			ctype = new CClassType(naked, t, generics);
			CACHE.add(type, generics, ctype);
		}
		return ctype;
	}

//	/**
//	 * Returns the singular instance of CClassType that represents this type union. string|int and int|string are both
//	 * considered the same type union, as they are first normalized into a canonical form.
//	 *
//	 * Use {@link #get(com.laytonsmith.core.constructs.CClassType...)} instead, to ensure type safety, unless absolutely
//	 * impossible (comes from user input, for instance).
//	 *
//	 * @param types
//	 * @return
//	 */
//	private static CClassType get(FullyQualifiedClassName... types) throws ClassNotFoundException {
//
//		SortedSet<FullyQualifiedClassName> t = new TreeSet<>(Arrays.asList(types));
//		FullyQualifiedClassName type
//				= FullyQualifiedClassName.forFullyQualifiedClass(StringUtils.Join(t, "|", e -> e.getFQCN()));
//		CClassType ctype = CACHE.get(type);
//		if(ctype == null) {
//			ctype = new CClassType(type, Target.UNKNOWN, false, null);
//			CACHE.put(type, ctype);
//		}
//		return ctype;
//	}
//
//	/**
//	 * Returns the singular instance of CClassType that represents this type union. string|int and int|string are both
//	 * considered the same type union, as they are first normalized into a canonical form.
//	 *
//	 * @param types
//	 * @return
//	 */
//	public static CClassType get(CClassType... types) throws ClassNotFoundException {
//		return get(Stream.of(types)
//				.map(e -> e.getFQCN())
//				.sorted()
//				.collect(Collectors.toSet())
//				.toArray(new FullyQualifiedClassName[types.length]));
//	}

	/**
	 * This function defines a brand new class type. This should exclusively be used in a class
	 * definition scenario, and never when simply looking up an existing class. The created
	 * CClassType is returned.
	 * @param fqcn
	 * @return
	 */
	public static CClassType defineClass(FullyQualifiedClassName fqcn, GenericDeclaration genericDeclaration) {
		try {
			CClassType type = new CClassType(fqcn, Target.UNKNOWN, true, genericDeclaration);
			CACHE.add(fqcn, null, type);
			return type;
		} catch (ClassNotFoundException ex) {
			throw new Error(ex);
		}
	}

	/**
	 *
	 * @param type This must be the fully qualified string name.
	 * @param t
	 */
	private CClassType(String type, Target t, GenericDeclaration genericDeclaration) throws ClassNotFoundException {
		this(FullyQualifiedClassName.forFullyQualifiedClass(type), t, false, genericDeclaration);
	}

	private static String formatName(FullyQualifiedClassName type, GenericDeclaration generics) {
		if(generics == null) {
			return type.getFQCN();
		} else {
			StringBuilder b = new StringBuilder();
			b.append(type.getFQCN());
			b.append("<");
			b.append(generics.toString());
			b.append(">");
			return b.toString();
		}
	}

	/**
	 * Creates a new CClassType
	 *
	 * @param type
	 * @param t
	 * @param newDefinition If true, this function MUST NOT throw a ClassNotFoundException.
	 * @param genericDeclaration The generic declaration for this class. May be null if no generics are being defined.
	 */
	@SuppressWarnings("ConvertToStringSwitch")
	private CClassType(FullyQualifiedClassName type, Target t, boolean newDefinition, GenericDeclaration genericDeclaration)
			throws ClassNotFoundException {
		super(formatName(type, genericDeclaration), ConstructType.CLASS_TYPE, t);
		isTypeUnion = type.isTypeUnion();
		fqcn = type;
		this.genericParameters = null;
		this.genericDeclaration = genericDeclaration;
		if(isTypeUnion) {
			// Split them out
			types.addAll(Stream.of(type.getFQCN().split("\\|"))
					.map(e -> FullyQualifiedClassName.forFullyQualifiedClass(e.trim())).collect(Collectors.toList()));
		} else {
			types.add(type);
		}

		if(!newDefinition) {
			boolean found = false;
			String localFQCN = fqcn.getFQCN();
			if(localFQCN.equals("auto") || localFQCN.equals("ms.lang.ClassType")) {
				// If we get here, we are within this class, and calling resolveNativeType won't work,
				// but anyways, we know we exist, so mark it as found. It is important to note, however,
				// if we end up defining more magic types within this class, this block needs to be updated.
				found = true;
			}
			// Do this to ensure at construction time that the class really does exist. We can't actually construct
			// the instance yet, because this might be the stack for the TYPE assignment, which means that this class
			// is not initialized yet. See the docs for instantiateInvalidType().
			// This works because we assume that resolveNativeTypes only uses the ClassMirror system. If that assumption
			// changes, we will need to basically re-implement that ourselves.
			if(!found) {
				if(isTypeUnion) {
					// For type unions, we need to find all component parts
					boolean foundAllTypeUnion = true;
					for(FullyQualifiedClassName c : types) {
						if(null == NativeTypeList.resolveNativeType(c.getFQCN())) {
							foundAllTypeUnion = false;
							break;
						}
					}
					if(foundAllTypeUnion) {
						found = true;
					}
				} else {
					if(fqcn.getNativeClass() != null) {
						found = true;
					} else {
						found = null != NativeTypeList.resolveNativeType(fqcn.getFQCN());
					}
				}
			}
			// TODO: When user types are added, we will need to do some more digging here, and probably need
			// to pass in the CompilerEnvironment somehow.

			if(!found) {
				throw new ClassNotFoundException("Could not find class of type " + type);
			}
		}
	}

	/**
	 * Creates a genericized version of an existing naked class definition.
	 * @param nakedType The naked type, that is, the one that contains just the generic declaration with no parameters.
	 * @param t Code target
	 * @param genericParameters The concrete generic parameters
	 */
	private CClassType(CClassType nakedType, Target t, GenericParameters genericParameters) {
		super(nakedType.getFQCN() + (genericParameters == null ? "" : genericParameters.toString()),
				ConstructType.CLASS_TYPE, t);
		if(nakedType.isTypeUnion) {
			throw new Error("Type union classes cannot have generic parameters");
		}
		GenericDeclaration declaration = nakedType.getGenericDeclaration();
		this.genericDeclaration = declaration; // same declaration as "parent" class
		isTypeUnion = false;
		fqcn = nakedType.fqcn;
		this.genericParameters = genericParameters;
	}

	/**
	 * While we would prefer to instantiate invalidType in the constructor, we can't, because this initializes the type,
	 * which occurs first when TYPE is initialized, that is, before the class is valid. Therefore, we cannot actually
	 * do that in the constructor, we need to lazy load it. We do take pains in the constructor to ensure that there is
	 * at least no way this will throw a ClassCastException, so given that, we are able to supress that exception here.
	 */
	private void instantiateInvalidType(Environment env) {
		if(this.invalidType != UNINITIALIZED) {
			return;
		}
		synchronized(this) {
			if(this.invalidType != UNINITIALIZED) {
				return;
			}
			@SuppressWarnings("LocalVariableHidesMemberVariable")
			String fqcn = this.fqcn.getFQCN();
			try {
				Mixed[] invalidType;
				if("auto".equals(fqcn)) {
					invalidType = null;
				} else if("ms.lang.ClassType".equals(fqcn)) {
					invalidType = new Mixed[]{this};
				} else {
					invalidType = new Mixed[this.types.size()];
					for(int i = 0; i < invalidType.length; i++) {
						// TODO: For now, we must use this mechanism, since we don't populate the ODT with
						// all the native classes. But once we do, we should remove this check entirely here.
						if(NativeTypeList.getNativeTypeList().contains(this.fqcn)) {
							invalidType[i] = NativeTypeList.getInvalidInstanceForUse(this.fqcn);
						} else {
							ObjectDefinitionTable odt = env.getEnv(CompilerEnvironment.class).getObjectDefinitionTable();
							ObjectDefinition od = odt.get(this.fqcn);
							invalidType[i] = new UserObject(Target.UNKNOWN, null, env, od, null);
						}
					}
				}
				this.invalidType = invalidType;
			} catch (ClassNotFoundException | ObjectDefinitionNotFoundException ex) {
				throw new Error(ex);
			}
		}
	}

	@Override
	public boolean isDynamic() {
		return false;
	}

	@Override
	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	public boolean equals(Object obj) {
		// Because we maintain a static list of singletons, we can short circuit this check. If obj is not == to
		// us, we are different objects. If this is ever not correct, we have a serious problem elsewhere, as this
		// assumption is held elsewhere in code.
		return this == obj;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	/**
	 * Returns true if there is more than one type in this type
	 *
	 * @return
	 */
	public boolean isTypeUnion() {
		return this.isTypeUnion;
	}

	/**
	 * Returns true if checkClass extends, implements, or otherwise derives from superClass
	 *
	 * @param checkClass
	 * @param superClass
	 * @return
	 */
	public static boolean doesExtend(CClassType checkClass, CClassType superClass) {
		if(checkClass.equals(superClass)) {
			// more efficient check
			return true;
		}
		if(checkClass.nativeClass != null && superClass.nativeClass != null
				&& superClass.nativeClass.isAssignableFrom(checkClass.nativeClass)) {
			// Since native classes are not allowed to extend multiple superclasees, but
			// in general, they are allowed to advertise that they do, for the sake of
			// methodscript, this can only be used to return true, if it returns true, it
			// definitely is, but if it returns false, that does not explicitly mean that
			// it doesn't. However, this check is faster, so we can do it and in 99% of
			// cases get a performance boost.
			return true;
		}
		for(CClassType tCheck : checkClass.getTypes()) {
			for(CClassType tSuper : superClass.getTypes()) {
				try {
					// TODO: This is currently being done in a very lazy way. It needs to be reworked.
					// For now, this is ok, but will not work once user types are added.
					Class cSuper = NativeTypeList.getNativeClass(tSuper.getFQCN());
					Class cCheck = NativeTypeList.getNativeClass(tCheck.getFQCN());
					if(!cSuper.isAssignableFrom(cCheck)) {
						return false;
					}
				} catch (ClassNotFoundException ex) {
					throw new RuntimeException(ex);
				}
			}
		}
		return true;
	}

	/**
	 * Returns true if this class extends the specified one
	 *
	 * @param superClass
	 * @return
	 */
	public boolean doesExtend(CClassType superClass) {
		return doesExtend(this, superClass);
	}

	/**
	 * Works like {@link #doesExtend(com.laytonsmith.core.constructs.CClassType, com.laytonsmith.core.constructs.CClassType)
	 * }, however rethrows the {@link ClassNotFoundException} that doesExtend throws as an {@link Error}. This should
	 * not be used unless the class names come from hardcoded values.
	 *
	 * @param checkClass
	 * @param superClass
	 * @return
	 */
	public static boolean unsafeDoesExtend(CClassType checkClass, CClassType superClass) {
		return doesExtend(checkClass, superClass);
	}

	/**
	 * Performs an unsafe check to see if this class extends the specified one
	 *
	 * @param superClass
	 * @return
	 */
	public boolean unsafeDoesExtend(CClassType superClass) {
		return unsafeDoesExtend(this, superClass);
	}

	/**
	 * Returns true if the specified class extends this one
	 *
	 * @param checkClass
	 * @return
	 * @throws ClassNotFoundException
	 */
	public boolean isExtendedBy(CClassType checkClass) throws ClassNotFoundException {
		return doesExtend(checkClass, this);
	}

	/**
	 * Performs an unsafe check to see if the specified class extends this one
	 *
	 * @param checkClass
	 * @return
	 */
	public boolean unsafeIsExtendedBy(CClassType checkClass) {
		return unsafeDoesExtend(checkClass, this);
	}

	@Override
	public CClassType[] getSuperclasses() {
		return new CClassType[]{Mixed.TYPE};
	}

	@Override
	public CClassType[] getInterfaces() {
		return CClassType.EMPTY_CLASS_ARRAY;
	}

	/**
	 * Returns the superclasses for the underlying type, not the superclasses for ClassType itself.
	 * @param env
	 * @return
	 */
	public CClassType[] getTypeSuperclasses(Environment env) {
		instantiateInvalidType(env);
		return Stream.of(invalidType).flatMap(e -> Stream.of(e.getSuperclasses()))
				.collect(Collectors.toSet()).toArray(CClassType.EMPTY_CLASS_ARRAY);
	}

	/**
	 *  Returns the interfaces for the underlying type, not the interfaces for ClassType itself.
	 * @param env
	 * @return
	 */
	public CClassType[] getTypeInterfaces(Environment env) {
		instantiateInvalidType(env);
		return Stream.of(invalidType).flatMap(e -> Stream.of(e.getInterfaces()))
				.collect(Collectors.toSet()).toArray(CClassType.EMPTY_CLASS_ARRAY);
	}

	/**
	 * Returns a set of individual types for this type. If it is a class union, multiple types will be returned in the
	 * set. Each of the CClassTypes within this set are guaranteed to not be a type union.
	 *
	 * This might be ok to make public if necessary in the future.
	 *
	 * @return
	 */
	protected Set<CClassType> getTypes() {
		Set<CClassType> t = new TreeSet<>(Comparator.comparing(CClassType::getFQCN));
		for(FullyQualifiedClassName type : types) {
			try {
				t.add(CClassType.get(type));
			} catch (ClassNotFoundException ex) {
				// This can't happen, because
				throw new Error(ex);
			}
		}
		return t;
	}

	/**
	 * Returns the package that this class is in. If the class is not in a package, or if this is a class union, null
	 * is returned.
	 * @return
	 */
	public CPackage getPackage() {
		if(isTypeUnion) {
			return null;
		}
		if(!val().contains(PATH_SEPARATOR)) {
			return null;
		}
		String[] parts = val().split(Pattern.quote(PATH_SEPARATOR));
		return new CPackage(Target.UNKNOWN, ArrayUtils.slice(parts, 0, parts.length - 2));
	}

	/**
	 * Returns the name of the class type without the package. If this is a type union, then each type is simplified,
	 * and returned as a string such as "int|string".
	 * @return
	 */
	public String getSimpleName() {
		return fqcn.getSimpleName();
	}

	@Override
	public String docs() {
		return "A ClassType is a value that represents an object type. This includes primitives or other value types.";
	}

	/**
	 * Returns the type if this is not a class union, and throws a CRECastException if it's a type union.
	 * @return
	 */
	private CClassType getOnlyTypeOrFail(Target t) {
		if(types.size() == 1) {
			return new TreeSet<>(getTypes()).first();
		}
		throw new CRECastException("This operation is not supported on a type union.", t);
	}

	public String getTypeDocs(Target t, Environment env) {
		getOnlyTypeOrFail(t);
		instantiateInvalidType(env);
		return invalidType[0].docs();
	}

	public Version getTypeSince(Target t, Environment env) {
		getOnlyTypeOrFail(t);
		instantiateInvalidType(env);
		return invalidType[0].since();
	}

	@Override
	public Version since() {
		return MSVersion.V3_3_1;
	}

	/**
	 * Returns the fully qualified class name for the class. Note that this is just the name of the class, not the
	 * complete type definition. See {@link #getTypeDefinition}.
	 * @return <code>ms.lang.ClassType</code> for instance.
	 */
	public FullyQualifiedClassName getFQCN() {
		return fqcn;
	}

	/**
	 * Returns the type definition, including generic definitions.
	 * @return <code>ms.lang.ClassType&gt;T&lt;</code> for instance.
	 */
	public String getTypeDefinition() {
		return val();
	}

	public boolean isEnum() {
		if("ms.lang.enum".equals(fqcn.getFQCN())) {
			// By default, this returns true when something is instanceof a thing, but in this case, we don't want
			// that, because ironically, ms.lang.enum is itself not an enum.
			return false;
		}
		return doesExtend(MEnumType.TYPE);
	}

	@Override
	public CClassType typeof() {
		return CClassType.TYPE;
	}

	// TODO: These getters will eventually be re-done to support static methods, but for now that is out of scope,
	// so we just specifically support enums for now.
	@Override
	public Mixed get(String index, Target t, Environment env) throws ConfigRuntimeException {
		if(isEnum()) {
			try {
				return NativeTypeList.getNativeEnumType(fqcn).get(index, t, env);
			} catch (ClassNotFoundException ex) {
				throw new RuntimeException(ex);
			}
		}
		throw new CREUnsupportedOperationException("Unsupported operation", t);
	}

	@Override
	public Mixed get(int index, Target t, Environment env) throws ConfigRuntimeException {
		if(isEnum()) {
			try {
				return NativeTypeList.getNativeEnumType(fqcn).get(index, t, env);
			} catch (ClassNotFoundException ex) {
				throw new RuntimeException(ex);
			}
		}
		throw new CREUnsupportedOperationException("Unsupported operation", t);
	}

	@Override
	public Mixed get(Mixed index, Target t, Environment env) throws ConfigRuntimeException {
		if(isEnum()) {
			try {
				return NativeTypeList.getNativeEnumType(fqcn).get(index, t, env);
			} catch (ClassNotFoundException ex) {
				throw new RuntimeException(ex);
			}
		}
		throw new CREUnsupportedOperationException("Unsupported operation", t);
	}

	@Override
	public Set<Mixed> keySet() {
		if(isEnum()) {
			try {
				return NativeTypeList.getNativeEnumType(fqcn).keySet();
			} catch (ClassNotFoundException ex) {
				throw new RuntimeException(ex);
			}
		}
		return new HashSet<>();
	}

	@Override
	public long size() {
		if(isEnum()) {
			try {
				return NativeTypeList.getNativeEnumType(fqcn).size();
			} catch (ClassNotFoundException ex) {
				throw new RuntimeException(ex);
			}
		}
		return 0;
	}

	@Override
	public boolean isAssociative() {
		return true;
	}

	@Override
	public boolean canBeAssociative() {
		return true;
	}

	@Override
	public Mixed slice(int begin, int end, Target t, Environment env) {
		throw new CREUnsupportedOperationException("Unsupported operation", t);
	}

	/**
	 * If this was constructed against a native class, we can do some optimizations in the course
	 * of operation. This may be null, and all code that uses this method must support the mechanisms if this
	 * is null anyways, but if it isn't null, then this can perhaps be used to help optimize.
	 * @return
	 */
	public Class<? extends Mixed> getNativeType() {
		return nativeClass;
	}

	@Override
	public boolean getBooleanValue(Target t) {
		return true;
	}

	/**
	 * Returns the generic declaration on the class itself.
	 *
	 * @return null if no generics were defined on this class, or else the {@link GenericDeclaration} for this ClassType.
	 */
	public GenericDeclaration getGenericDeclaration() {
		return genericDeclaration;
	}

	/**
	 * Returns the generic parameters on the instance of the class.
	 * @return null if no generics are defined on this class, or they were, but it's the naked class.
	 */
	public GenericParameters getGenericParameters() {
		return this.genericParameters;
	}

	private static class ClassTypeCache {

		private Map<Pair<FullyQualifiedClassName, GenericParameters>, CClassType> cache;

		public ClassTypeCache() {
			cache = Collections.synchronizedMap(new HashMap<>());
		}

		/**
		 * Adds a new class to the cache.
		 * @param fqcn The fully qualified class name
		 * @param parameters The parameters for this instance. This may be null, both if the class has no generic
		 *                   definition, but also if this is the naked class.
		 * @param type The CClassType.
		 */
		public void add(FullyQualifiedClassName fqcn, GenericParameters parameters, CClassType type) {
			cache.put(new Pair<>(fqcn, parameters), type);
		}

		public List<GenericParameters> getGenericsByFQCN(FullyQualifiedClassName fqcn) {
			List<GenericParameters> ret = new ArrayList<>();
			Set<Pair<FullyQualifiedClassName, GenericParameters>> keySet = cache.keySet();
			synchronized(cache) {
				for(Pair<FullyQualifiedClassName, GenericParameters> lhs : keySet) {
					if(lhs.getKey().equals(fqcn)) {
						ret.add(lhs.getValue());
					}
				}
			}
			return ret;
		}

		/**
		 * Returns the naked class, that is, the class without a defined parameter set. This may
		 * return null if the class has not been defined at all. Note that all classes without generic
		 * parameters are considered "naked", but then those would have been added with a null parameter
		 * set anyways, which should be equivalent.
		 * @param fqcn
		 * @return
		 */
		public CClassType getNakedClassType(FullyQualifiedClassName fqcn) {
			return get(fqcn, null);
		}

		/**
		 * Gets the CClassType instance for this FQCN and parameter set.
		 * @param fqcn The fully qualified class name
		 * @param declaration The parameter declaration. Null if this is a type without a parameter declaration, or
		 *                    if you wish to get the naked class.
		 */
		public CClassType get(FullyQualifiedClassName fqcn, GenericParameters declaration) {
			return cache.get(new Pair<>(fqcn, declaration));
		}
	}
}
