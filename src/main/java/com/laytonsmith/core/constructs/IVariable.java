/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.laytonsmith.core.constructs;

/**
 *
 * @author layton
 */
public class IVariable extends Construct implements Cloneable{
    
    public static final long serialVersionUID = 1L;
    final private String name;
    private Construct var_value;

    public IVariable(String name, Construct value, Target t){
        super(name, ConstructType.IVARIABLE, t);
        this.var_value = value;
        this.name = name;
    }
    public IVariable(String name, Target t){
        super(name, ConstructType.IVARIABLE, t);
        this.var_value = new CString("", t);
        this.name = name;
    }
    @Override
    public IVariable clone() throws CloneNotSupportedException{
        IVariable clone = (IVariable) super.clone();
        if(this.var_value != null) clone.var_value = this.var_value.clone();
        return (IVariable) clone;
    }
    public String getName(){
        return name;
    }
    @Override
    public boolean isDynamic() {
        return true;
    }
    public Construct ival(){
        var_value.setTarget(getTarget());
        return var_value;
    }

    public void setIval(Construct c){
        var_value = c;
    }
    
    
    @Override
    public String toString() {
        return this.name + ":(" + this.ival().getClass().getSimpleName() + ") '" + this.ival().val() + "'";
    }

    @Override
    public String val(){
        return var_value.val();
    }

}
