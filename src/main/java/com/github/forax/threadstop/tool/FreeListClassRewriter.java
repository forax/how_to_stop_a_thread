package com.github.forax.threadstop.tool;

import com.github.forax.threadstop.FreezeList;

import java.io.IOException;
import java.lang.classfile.Annotation;
import java.lang.classfile.ClassFile;
import java.lang.classfile.MethodModel;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class FreeListClassRewriter {
  public static void main(String[] args) throws IOException {
    var freezeListPath = Path.of(FreezeList.class.getName().replace('.', '/') + ".class");
    var path = Path.of("target", "classes").resolve(freezeListPath);
    var classModel = ClassFile.of().parse(path);
    if (classModel.majorVersion() > Runtime.version().feature() + 44) {
      throw new AssertionError("unknown bytecode version " + classModel.majorVersion());
    }
    var bytecode = ClassFile.of().transform(classModel, (classBuilder, classElement) -> {
      //System.out.println(" ." + classElement);
      switch (classElement) {
        case MethodModel methodModel -> {
          classBuilder.transformMethod(methodModel, (methodBuilder, methodElement) -> {
            //System.out.println("   ." + methodElement);
            switch (methodElement) {
              case RuntimeVisibleAnnotationsAttribute annotationsAttribute -> {
                var annotations = annotationsAttribute.annotations();
                //System.out.println("     ." + annotations);
                if (annotations.stream().noneMatch(a -> a.className().equalsString("Lcom/github/forax/threadstop/FreezeList$Scoped;"))) {
                  methodBuilder.with(annotationsAttribute);
                  return;
                }
                var newAnnotations = new ArrayList<>(annotations);
                var cp = classBuilder.constantPool();
                newAnnotations.add(Annotation.of(cp.utf8Entry("Ljdk/internal/misc/ScopeMemoryAccess$Scoped;")));
                var newAttribute = RuntimeVisibleAnnotationsAttribute.of(newAnnotations);
                //System.out.println("        new attribute " + newAttribute);
                methodBuilder.with(newAttribute);
              }
              default -> methodBuilder.with(methodElement);
            }
          });
        }
        default -> classBuilder.with(classElement);
      }
    });

    var errors = ClassFile.of().verify(bytecode);
    if (!errors.isEmpty()) {
      throw new AssertionError("error while rewriting " + path);
    }

    Files.write(path, bytecode);
    System.out.println(path + " rewriten");
  }
}
