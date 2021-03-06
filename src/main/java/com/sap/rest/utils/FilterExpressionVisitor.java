package com.sap.rest.utils;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.edm.EdmEnumType;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.edm.primitivetype.EdmString;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.expression.*;

import java.util.List;
import java.util.Locale;

public class FilterExpressionVisitor implements ExpressionVisitor<Object> {
    private Entity entity;

    public FilterExpressionVisitor(Entity entity) {
        this.entity = entity;
    }

    @Override
    public Object visitTypeLiteral(EdmType edmType) throws ExpressionVisitException, ODataApplicationException {
        return null;
    }

    @Override
    public Object visitAlias(String s) throws ExpressionVisitException, ODataApplicationException {
        return null;
    }

    @Override
    public Object visitEnum(EdmEnumType edmEnumType, List<String> list) throws ExpressionVisitException, ODataApplicationException {
        return null;
    }

    @Override
    public Object visitLambdaExpression(String s, String s1, Expression expression) throws ExpressionVisitException, ODataApplicationException {
        return null;
    }

    @Override
    public Object visitLambdaReference(String s) throws ExpressionVisitException, ODataApplicationException {
        return null;
    }

    @Override
    public Object visitMember(UriInfoResource uriInfoResource) throws ExpressionVisitException, ODataApplicationException {
        List<UriResource> uriResourceParts = uriInfoResource.getUriResourceParts();

        if(uriResourceParts.size() == 1 && uriResourceParts.get(0) instanceof UriResourcePrimitiveProperty) {
            UriResourcePrimitiveProperty uriResourcePrimitiveProperty = (UriResourcePrimitiveProperty) uriResourceParts.get(0);
            return entity.getProperty(uriResourcePrimitiveProperty.getProperty().getName()).getValue();
        }
        else {
            throw new ODataApplicationException("Only primitive properties are implemented in filter expressions", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }
    }

    @Override
    public Object visitLiteral(Literal literal) throws ExpressionVisitException, ODataApplicationException {
        String literalAsString = literal.getText();
        if(literal.getType() instanceof EdmString) {
            String stringLiteral = "";
            if (literal.getText().length() > 2) {
                stringLiteral = literalAsString.substring(1, literalAsString.length() - 1);
            }
            return stringLiteral;
        }
        else {
            try {
                return Integer.valueOf(literalAsString);
            } catch (NumberFormatException e) {
                throw new ODataApplicationException("Only Edm.Int32 and Edm.String literals are implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
            }
        }
    }

    @Override
    public Object visitUnaryOperator(UnaryOperatorKind unaryOperatorKind, Object object) throws ExpressionVisitException, ODataApplicationException {
        if(unaryOperatorKind == UnaryOperatorKind.NOT && object instanceof Boolean) {
            return !(Boolean) object;
        }
        else  if (unaryOperatorKind == UnaryOperatorKind.MINUS && object instanceof Integer) {
            return -(Integer) object;
        }
        throw new ODataApplicationException("Invalid type for unary operator", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
    }

    private Object evaluateBooleanOperation(BinaryOperatorKind binaryOperatorKind, Object left, Object right) throws ODataApplicationException {
        if (left instanceof Boolean && right instanceof Boolean) {
            Boolean valueLeft = (Boolean) left;
            Boolean valueRight = (Boolean) right;

            if(binaryOperatorKind == BinaryOperatorKind.AND)
                return valueLeft && valueRight;
            else
                return valueLeft || valueRight;
        }
        else
            throw new ODataApplicationException("Boolean operations needs two numeric operands", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
    }

    private Object evaluateComparisonOperation(BinaryOperatorKind binaryOperatorKind, Object left, Object right) throws ODataApplicationException {
        if(left.getClass().equals(right.getClass())) {
            Integer result;

            if(left instanceof Integer)
                result = ((Comparable<Integer>) (Integer) left).compareTo((Integer) right);
            else if (left instanceof String)
                result = ((Comparable<String>) (String) left).compareTo((String) right);
            else if (left instanceof Boolean)
                result = ((Comparable<Boolean>) (Boolean) left).compareTo((Boolean) right);
            else
                throw new ODataApplicationException("Class " + left.getClass().getCanonicalName() + " not expected", HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);


            if(null == binaryOperatorKind)
                return !(result == 0);
            else switch (binaryOperatorKind) {
                case EQ:
                    return result == 0;
                case GE:
                    return result >= 0;
                case LE:
                    return result <= 0;
                case GT:
                    return result > 0;
                case LT:
                    return result < 0;
                default:
                    return !(result == 0);
            }
        }
        else
            throw new ODataApplicationException("Comparison needs two equal types", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
    }

    private Object evaluateArithmeticOperation(BinaryOperatorKind binaryOperatorKind, Object left, Object right) throws ODataApplicationException {
        if (left instanceof Integer && right instanceof Integer) {
            Integer valueLeft = (Integer) left;
            Integer valueRight = (Integer) right;

            if (null == binaryOperatorKind)
                return valueLeft % valueRight;
            else switch (binaryOperatorKind) {
                case ADD:
                    return valueLeft + valueRight;
                case SUB:
                    return valueLeft - valueRight;
                case DIV:
                    return valueLeft / valueRight;
                case MUL:
                    return valueLeft * valueRight;
                default:
                    return valueLeft % valueRight;
            }
        }
        else
            throw new ODataApplicationException("Arithmetic operations needs two numeric operands", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
    }

    @Override
    public Object visitBinaryOperator(BinaryOperatorKind binaryOperatorKind, Object object, Object t1) throws ExpressionVisitException, ODataApplicationException {
        if(null == binaryOperatorKind)
            throw new ODataApplicationException("Binary operation " + binaryOperatorKind.name() + " is not implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        else switch (binaryOperatorKind) {
            case ADD:
            case SUB:
            case DIV:
            case MUL:
            case MOD:
                return evaluateArithmeticOperation(binaryOperatorKind, object, t1);
            case EQ:
            case GE:
            case GT:
            case LE:
            case LT:
            case NE:
                return evaluateComparisonOperation(binaryOperatorKind, object, t1);
            case AND:
            case OR:
                return evaluateBooleanOperation(binaryOperatorKind, object, t1);
            default:
                throw new ODataApplicationException("Binary operation " + binaryOperatorKind.name() + " is not implemented", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }
    }

    @Override
    public Object visitMethodCall(MethodKind methodKind, List<Object> list) throws ExpressionVisitException, ODataApplicationException {
        return null;
    }
}