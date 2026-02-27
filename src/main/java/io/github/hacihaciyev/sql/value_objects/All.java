package io.github.hacihaciyev.sql.value_objects;

record All(){
    static final String value = "*";
    
    @Override
    public final String toString() {
        return value;
    }
}
