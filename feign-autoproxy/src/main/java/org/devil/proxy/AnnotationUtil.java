package org.devil.proxy;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yaojun
 * 2020/8/20 15:52
 */
public class AnnotationUtil {


    public static javassist.bytecode.annotation.Annotation createAnnotation(Annotation annotation, ConstPool constPool) throws NotFoundException, InvocationTargetException, IllegalAccessException {
        //create annotation
        javassist.bytecode.annotation.Annotation proxyAnnotation = new javassist.bytecode.annotation.Annotation(annotation.annotationType().getName(),constPool);

        //add annotation
        Method[] methods = annotation.annotationType().getDeclaredMethods();

        for (Method method : methods) {
            MemberValue value = createMember(constPool, ClassPool.getDefault().get(method.getReturnType().getName()), method.invoke(annotation));
            if (value != null) {
                proxyAnnotation.addMemberValue(method.getName(), value);
            }
        }

        return proxyAnnotation;
    }

    private static MemberValue createMember(ConstPool constPool, CtClass memberType, Object value) throws NotFoundException, InvocationTargetException, IllegalAccessException {

            if (memberType == CtClass.booleanType) {
                return createBoolean(constPool,value);
            }

            if (memberType == CtClass.byteType) {
                return createByte(constPool, value);
            }

            if (memberType == CtClass.charType) {
                return createChar(constPool, value);
            }

            if (memberType == CtClass.shortType) {
                return createShort(constPool, value);
            }

            if (memberType == CtClass.intType) {
                return createInteger(constPool, value);
            }

            if (memberType == CtClass.longType) {
                return createLong(constPool, value);
            }

            if (memberType == CtClass.floatType) {
                return createFloat(constPool, value);
            }

            if (memberType == CtClass.doubleType) {
                return createDouble(constPool, value);
            }

            if (memberType.getName().equals(Class.class.getName())) {
                return createClass(constPool, value);

            }

            if (memberType.getName().equals(String.class.getName())) {
                return createString(constPool, value);

            }

            if (memberType.isArray()) {
                return createArray(constPool,value,memberType);
            }

            if (memberType.isEnum()) {
                return createEnum(constPool, value);
            }

            if (memberType.isAnnotation()){
                javassist.bytecode.annotation.Annotation annotation = createAnnotation((Annotation)value,constPool);
                return new AnnotationMemberValue(annotation,constPool);
            }

            return null;
        }

    private static MemberValue createBoolean(ConstPool constPool,Object value){
        BooleanMemberValue memberValue = new BooleanMemberValue(constPool);
        if (value != null) {
            memberValue.setValue((Boolean) value);
        }
        return memberValue;
    }

    private static MemberValue createByte(ConstPool constPool,Object value){
        ByteMemberValue memberValue = new ByteMemberValue(constPool);
        if (value != null) {
            memberValue.setValue((byte) value);
        }
        return memberValue;
    }

    private static MemberValue createChar(ConstPool constPool,Object value){
        CharMemberValue memberValue = new CharMemberValue(constPool);
        if (value != null) {
            memberValue.setValue((char) value);
        }
        return memberValue;
    }

    private static MemberValue createShort(ConstPool constPool,Object value){
        ShortMemberValue memberValue = new ShortMemberValue(constPool);
        if (value != null) {
            memberValue.setValue((short) value);
        }
        return memberValue;
    }

    private static MemberValue createInteger(ConstPool constPool,Object value){
        IntegerMemberValue memberValue = new IntegerMemberValue(constPool);
        if (value != null) {
            memberValue.setValue((int) value);
        }
        return memberValue;
    }

    private static MemberValue createLong(ConstPool constPool,Object value){
        LongMemberValue memberValue = new LongMemberValue(constPool);
        if (value != null) {
            memberValue.setValue((long) value);
        }
        return memberValue;
    }

    private static MemberValue createFloat(ConstPool constPool,Object value){
        FloatMemberValue memberValue = new FloatMemberValue(constPool);
        if (value != null) {
            memberValue.setValue((float) value);
        }
        return memberValue;
    }

    private static MemberValue createDouble(ConstPool constPool,Object value){
        DoubleMemberValue memberValue = new DoubleMemberValue(constPool);
        if (value != null) {
            memberValue.setValue((double) value);
        }
        return memberValue;
    }

    private static MemberValue createString(ConstPool constPool,Object value){
        StringMemberValue memberValue = new StringMemberValue(constPool);
        memberValue.setValue((String)value);
        return memberValue;
    }

    private static MemberValue createEnum(ConstPool constPool,Object value){
        EnumMemberValue emv = new EnumMemberValue(constPool);
        emv.setType(value.getClass().getName());
        emv.setValue(((Enum) value).name());
        return emv;
    }

    private static MemberValue createClass(ConstPool constPool,Object value){
        ClassMemberValue memberValue = new ClassMemberValue(constPool);
        if (value instanceof Class){
            memberValue.setValue(((Class) value).getName());
        }else if (value instanceof String){
            memberValue.setValue(((String) value));
        }else {
            throw new IllegalArgumentException("value is not correct "+value.getClass());
        }
        return memberValue;
    }

    private static MemberValue createArray(ConstPool constPool,Object value,CtClass type) throws NotFoundException, InvocationTargetException, IllegalAccessException {
        ArrayMemberValue arrayMemberValue = new ArrayMemberValue(constPool);
        CtClass ctClass = type.getComponentType();
        List<MemberValue> list = new ArrayList<>();
        for (Object o : ((Object[]) value)) {
            MemberValue val = createMember(constPool, ctClass, o);
            if (val != null) {
                list.add(val);
            }
        }
        arrayMemberValue.setValue(list.toArray(new MemberValue[0]));
        return arrayMemberValue;
    }
}
