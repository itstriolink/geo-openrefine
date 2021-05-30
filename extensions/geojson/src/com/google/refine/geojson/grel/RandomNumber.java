package com.google.refine.geojson.grel;

import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class RandomNumber implements Function {
    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2 && args[0] != null && args[0] instanceof Number
                && args[1] != null && args[1] instanceof Number
                && ((Number) args[0]).intValue()<((Number) args[1]).intValue()) {
            return ThreadLocalRandom.current().nextInt(((Number) args[0]).intValue(), ((Number) args[1]).intValue()+1);
        }

        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects two numbers, the first must be less than the second");
    }

    @Override
    public String getDescription() {
        return "Returns a pseudo-random integer between the lower and upper bound (inclusive)";
    }

    @Override
    public String getParams() { return "lower bound, upper bound"; }

    @Override
    public String getReturns() {
        return "Number";
    }
}