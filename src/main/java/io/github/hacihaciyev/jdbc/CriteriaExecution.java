package io.github.hacihaciyev.jdbc;

import io.github.hacihaciyev.sql.Criteria;
import io.github.hacihaciyev.util.Result;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class CriteriaExecution<T> {
    private final JetQuerious jq;
    private final Criteria query;
    private final ResultSetExtractor<T> extractor;
    private final ResultSetType rsType;
    private final Object[] args;

    CriteriaExecution(JetQuerious jq, Criteria query, ResultSetExtractor<T> extractor) {
        this(
            jq,
            requireNonNull(query, "Criteria query cannot be null"),
            requireNonNull(extractor, "Extractor cannot be null"),
            ResultSetType.FORWARD_ONLY_READ_ONLY,
            new Object[0]
        );
    }

    private CriteriaExecution(
        JetQuerious jq, Criteria query, ResultSetExtractor<T> extractor,
        ResultSetType rsType, Object[] args
    ) {
        this.jq        = jq;
        this.query     = query;
        this.extractor = extractor;
        this.rsType    = rsType;
        this.args      = args;
    }

    public CriteriaExecution<T> resultSetType(ResultSetType rsType) {
        return new CriteriaExecution<>(jq, query, extractor, requireNonNull(rsType, "ResultSetType cannot be null"), args);
    }

    public CriteriaExecution<T> args(Object... args) {
        return new CriteriaExecution<>(jq, query, extractor, rsType, requireNonNull(args, "args cannot be null").clone());
    }

    public Result<T, Exception> one() {
        return jq.executeCriteriaOne(query, extractor, rsType, args);
    }

    public Result<Optional<T>, Exception> option() {
        return jq.executeCriteriaOption(query, extractor, rsType, args);
    }

    public Result<List<T>, Exception> many() {
        return jq.executeCriteriaMany(query, extractor, rsType, args);
    }
}