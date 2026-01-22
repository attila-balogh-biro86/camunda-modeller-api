package com.ferrovial.tsm.dmn.generation.expression.render;


import com.ferrovial.tsm.dmn.generation.expression.api.Expression;

/**
 * Interface for rendering expressions to different output formats.
 *
 * @param <T> the output type of the renderer
 */
public interface ExpressionRenderer<T> {

    /**
     * Renders an expression to the target format.
     *
     * @param expression the expression to render
     * @return the rendered output
     */
    T render(Expression expression);

    /**
     * Returns the name of this renderer.
     */
    String getName();

    /**
     * Returns a description of the output format.
     */
    String getDescription();
}
