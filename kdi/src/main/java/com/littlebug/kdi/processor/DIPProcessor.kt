package com.littlebug.kdi.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.littlebug.kdi.annotation.AutoInject
import com.littlebug.kdi.annotation.Inject
import com.littlebug.kdi.annotation.Injectable
import com.littlebug.kdi.annotation.Module
import com.littlebug.kdi.annotation.Provides
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

internal class DIPProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
): SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(AutoInject::class.qualifiedName!!)
        symbols.forEach { symbol ->
            if (symbol is KSClassDeclaration) {
                generateDISetupCode(symbol)
            }
        }
        processInjectable(resolver)
        processModules(resolver)
        processInjectAnnotations(resolver)
        return emptyList()
    }

    private fun processInjectable(resolver: Resolver) {
        val injectableSymbols = resolver.getSymbolsWithAnnotation(Injectable::class.qualifiedName!!)
        injectableSymbols.forEach { symbol ->
            if (symbol is KSClassDeclaration) {
                registerInjectable(symbol)
            }
        }
    }

    private fun registerInjectable(classDeclaration: KSClassDeclaration) {
        val className = classDeclaration.toClassName()
        val fileSpec = FileSpec.builder(className.packageName, "${className.simpleName}Provider")
            .addType(
                TypeSpec.classBuilder("${className.simpleName}Provider")
                    .addFunction(
                        FunSpec.builder("provide${className.simpleName}")
                            .returns(className)
                            .addStatement("return ${className.simpleName}()")
                            .build()
                    )
                    .build()
            )
            .build()
        fileSpec.writeTo(codeGenerator, Dependencies.ALL_FILES)
    }

    private fun processModules(resolver: Resolver) {
        val moduleSymbols = resolver.getSymbolsWithAnnotation(Module::class.qualifiedName!!)
        moduleSymbols.forEach { symbol ->
            if (symbol is KSClassDeclaration) {
                processModule(symbol)
            }
        }
    }

    private fun processModule(classDeclaration: KSClassDeclaration) {
        classDeclaration.getAllFunctions().forEach { function ->
            function.annotations.forEach { annotation ->
                when (annotation.shortName.asString()) {
                    Provides::class.simpleName -> registerProvider(function)
                }
            }
        }
    }

    private fun registerProvider(function: KSFunctionDeclaration) {
        val returnType = function.returnType?.resolve()?.declaration as? KSClassDeclaration
        val returnTypeName = returnType?.toClassName()
        val providerName = function.simpleName.asString()

        if (returnTypeName != null) {
            val fileSpec = FileSpec.builder(returnTypeName.packageName, "${returnTypeName.simpleName}Provider")
                .addType(
                    TypeSpec.classBuilder("${returnTypeName.simpleName}Provider")
                        .addFunction(
                            FunSpec.builder(providerName)
                                .returns(returnTypeName)
                                .addStatement("return ${returnTypeName.simpleName}()")
                                .build()
                        )
                        .build()
                )
                .build()
            fileSpec.writeTo(codeGenerator, Dependencies.ALL_FILES)
        }
    }

    private fun processInjectAnnotations(resolver: Resolver) {
        val injectSymbols = resolver.getSymbolsWithAnnotation(Inject::class.qualifiedName!!)
        injectSymbols.forEach { symbol ->
            when (symbol) {
                is KSClassDeclaration -> processInjectConstructor(symbol)
                is KSPropertyDeclaration -> processInjectProperty(symbol)
            }
        }
    }

    private fun processInjectConstructor(classDeclaration: KSClassDeclaration) {
        classDeclaration.primaryConstructor?.let { constructor ->
            // Generate code to handle constructor injection
            val className = classDeclaration.toClassName()
            val params = constructor.parameters.joinToString { param ->
                val paramType = param.type.resolve().toTypeName()
                "${param.name!!.asString()}: $paramType"
            }
            val constructorCall = constructor.parameters.joinToString { param ->
                param.name!!.asString()
            }

            val fileSpec = FileSpec.builder(className.packageName, "${className.simpleName}Factory")
                .addType(
                    TypeSpec.classBuilder("${className.simpleName}Factory")
                        .addFunction(
                            FunSpec.builder("create")
                                .returns(className)
                                .addStatement("return ${className.simpleName}($constructorCall)")
                                .build()
                        )
                        .build()
                )
                .build()
            fileSpec.writeTo(codeGenerator, Dependencies.ALL_FILES)
        }
    }

    private fun processInjectProperty(property: KSPropertyDeclaration) {
        // Generate code to handle property injection
        val className = (property.parentDeclaration as KSClassDeclaration).toClassName()
        val propertyName = property.simpleName.asString()
        val propertyType = property.type.resolve().toTypeName()

        val fileSpec = FileSpec.builder(className.packageName, "${className.simpleName}Injector")
            .addType(
                TypeSpec.classBuilder("${className.simpleName}Injector")
                    .addFunction(
                        FunSpec.builder("inject$propertyName")
                            .addParameter("instance", className)
                            .addParameter(propertyName, propertyType)
                            .addStatement("instance.$propertyName = $propertyName")
                            .build()
                    )
                    .build()
            )
            .build()
        fileSpec.writeTo(codeGenerator, Dependencies.ALL_FILES)
    }

    private fun generateDISetupCode(classDeclaration: KSClassDeclaration) {
        val className = classDeclaration.simpleName.asString()
        val packageName = classDeclaration.packageName.asString()
        val fileName = "${className}DISetup"

        val fileSpec = FileSpec.builder(packageName, fileName)
        val classSpec = TypeSpec.classBuilder(fileName)
            .addFunction(
                FunSpec.builder("setupDI")
                    .addParameter("activity", ClassName("androidx.appcompat.app", "AppCompatActivity"))
                    .addStatement("val dependencyGraph = DependencyGraph()")
                    .addStatement("val lifecycleAwareDI = LifecycleAwareDI(dependencyGraph)")
                    .addStatement("lifecycleAwareDI.inject<$className>(activity)")
                    .build()
            )
            .build()

        fileSpec.addType(classSpec).build().writeTo(codeGenerator, Dependencies.ALL_FILES)
    }
}