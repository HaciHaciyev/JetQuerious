package io.github.hacihaciyev.sql.expressions;

public sealed interface Func extends Expr {
    
    sealed interface AggregateFunc {}
    
    //sealed interface StringFunc {}
    
    //sealed interface DataTimeFunc {}
    
    //sealed interface ConditionalFunc {}
    
    public record Count() implements Func, AggregateFunc {}
    
    public record Sum() implements Func, AggregateFunc {}
    
    public record Avg() implements Func, AggregateFunc {}
    
    public record Min() implements Func, AggregateFunc {}
    
    public record Max() implements Func, AggregateFunc {}
}