/*-
 * #%L
 * BroadleafCommerce Common Libraries
 * %%
 * Copyright (C) 2009 - 2025 Broadleaf Commerce
 * %%
 * Licensed under the Broadleaf Fair Use License Agreement, Version 1.0
 * (the "Fair Use License" located  at http://license.broadleafcommerce.org/fair_use_license-1.0.txt)
 * unless the restrictions on use therein are violated and require payment to Broadleaf in which case
 * the Broadleaf End User License Agreement (EULA), Version 1.1
 * (the "Commercial License" located at http://license.broadleafcommerce.org/commercial_license-1.1.txt)
 * shall apply.
 * 
 * Alternatively, the Commercial License may be replaced with a mutually agreed upon license (the "Custom License")
 * between you and Broadleaf Commerce. You may not use this file except in compliance with the applicable license.
 * #L%
 */
package org.broadleafcommerce.common.extensibility.jpa.copy;

import org.apache.commons.lang.StringUtils;
import org.broadleafcommerce.common.extensibility.jpa.convert.BroadleafClassTransformer;
import org.broadleafcommerce.common.logging.LifeCycleEvent;
import org.broadleafcommerce.common.logging.SupportLogManager;
import org.broadleafcommerce.common.logging.SupportLogger;

import java.io.ByteArrayInputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;

/**
 * This class transformer will copy fields, methods, and interface definitions from a source class to a target class,
 * based on the xformTemplates map. It will fail if it encouters any duplicate definitions.
 * 
 * @author Andre Azzolini (apazzolini)
 * @deprecated do not use this class, use {@link DirectCopyClassTransformer} instead
 */
@Deprecated
public class AnnotationsCopyClassTransformer implements BroadleafClassTransformer {
    protected SupportLogger logger;
    
    protected String moduleName;
    protected Map<String, String> xformTemplates = new HashMap<String, String>();
    
    protected static List<String> transformedMethods = new ArrayList<String>();
    
    public AnnotationsCopyClassTransformer(String moduleName) {
        this.moduleName = moduleName;
        logger = SupportLogManager.getLogger(moduleName, this.getClass());
    }
    
    @Override
    public void compileJPAProperties(Properties props, Object key) throws Exception {
        // When simply copying properties over for Java class files, JPA properties do not need modification
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, 
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        // Lambdas and anonymous methods in Java 8 do not have a class name defined and so no transformation should be done
        if (className == null) {
            return null;
        }
        String convertedClassName = className.replace('/', '.');
        
        if (xformTemplates.containsKey(convertedClassName)) {
            String xformKey = convertedClassName;
            String[] xformVals = xformTemplates.get(xformKey).split(",");
            logger.lifecycle(LifeCycleEvent.START, String.format("Transform - Copying annotations into [%s] from [%s]", xformKey,
                    StringUtils.join(xformVals, ",")));
            CtClass clazz = null;
            try {
                // Load the destination class and defrost it so it is eligible for modifications
                ClassPool classPool = ClassPool.getDefault();
                clazz = classPool.makeClass(new ByteArrayInputStream(classfileBuffer), false);
                clazz.defrost();

                for (String xformVal : xformVals) {
                    // Load the source class
                    String trimmed = xformVal.trim();
                    classPool.appendClassPath(new LoaderClassPath(Class.forName(trimmed).getClassLoader()));
                    CtClass template = classPool.get(trimmed);

                    // Copy over all declared annotations from fields from the template class
                    // Note that we do not copy over fields with the @NonCopiedField annotation
                    CtField[] fieldsToCopy = template.getDeclaredFields();
                    for (CtField field : fieldsToCopy) {
                        if (field.hasAnnotation(NonCopied.class)) {
                            logger.debug(String.format("Not copying annotation from field [%s]", field.getName()));
                        } else {
                            logger.debug(String.format("Copying annotation from field [%s]", field.getName()));

                            ConstPool constPool = clazz.getClassFile().getConstPool();

                            CtField fieldFromMainClass = clazz.getField(field.getName());

                            for (Object o : field.getFieldInfo().getAttributes()) {
                                if (o instanceof AnnotationsAttribute) {
                                    AnnotationsAttribute templateAnnotations = (AnnotationsAttribute) o;
                                    //have to make a copy of the annotations from the target
                                    AnnotationsAttribute copied = (AnnotationsAttribute) templateAnnotations.copy(constPool, null);
                                    
                                    //add all the copied annotations into the target class's field.
                                    for (Object attribute : fieldFromMainClass.getFieldInfo().getAttributes()) {
                                        if (attribute instanceof AnnotationsAttribute) {
                                            for (Annotation annotation : copied.getAnnotations()) {
                                                ((AnnotationsAttribute) attribute).addAnnotation(annotation);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                logger.lifecycle(LifeCycleEvent.END, String.format("Transform - Copying annotations into [%s] from [%s]", xformKey,
                                    StringUtils.join(xformVals, ",")));
                return clazz.toBytecode();
            } catch (Exception e) {
                throw new RuntimeException("Unable to transform class", e);
            } finally {
                if (clazz != null) {
                    clazz.detach();
                }
            }
        }
        
        return null;
    }

    /**
     * This method will do its best to return an implementation type for a given classname. This will allow weaving
     * template classes to have initialized values.
     * 
     * We provide default implementations for List, Map, and Set, and will attempt to utilize a default constructor for
     * other classes.
     * 
     * If the className contains an '[', we will return null.
     */
    protected String getImplementationType(String className) {
        if (className.equals("java.util.List")) {
            return "java.util.ArrayList";
        } else if (className.equals("java.util.Map")) {
            return "java.util.HashMap";
        } else if (className.equals("java.util.Set")) {
            return "java.util.HashSet";
        } else if (className.contains("[")) {
            return null;
        }

        return className;
    }

    protected String methodDescription(CtMethod method) {
        return method.getDeclaringClass().getName() + "|" + method.getName() + "|" + method.getSignature();
    }
    
    public Map<String, String> getXformTemplates() {
        return xformTemplates;
    }

    public void setXformTemplates(Map<String, String> xformTemplates) {
        this.xformTemplates = xformTemplates;
    }
    
}
