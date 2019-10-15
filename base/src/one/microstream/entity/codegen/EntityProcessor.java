
package one.microstream.entity.codegen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

import one.microstream.chars.VarString;
import one.microstream.chars.XChars;
import one.microstream.entity.Entity;
import one.microstream.entity.EntityException;


public class EntityProcessor extends AbstractProcessor
{
	private final static String     OPTION_PREFIX        = "microstream.entity.";
	private final static String     OPTION_UPDATER       = OPTION_PREFIX.concat("updater");
	private final static String     OPTION_HASHEQUALATOR = OPTION_PREFIX.concat("hashequalator");
	
	private List<ExecutableElement> javaLangObjectMethods;
	private TypeMirror              runtimeExceptionType;
	
	public EntityProcessor()
	{
		super();
	}
	
	@Override
	public SourceVersion getSupportedSourceVersion()
	{
		return SourceVersion.latestSupported();
	}
	
	@Override
	public Set<String> getSupportedAnnotationTypes()
	{
		/*
		 * 'Hack' to process all elements, not only annotated classes, since we don't use annotations.
		 */
		return Collections.singleton("*");
	}
	
	@Override
	public Set<String> getSupportedOptions()
	{
		final Set<String> set = new HashSet<>();
		set.add(OPTION_UPDATER);
		set.add(OPTION_HASHEQUALATOR);
		return set;
	}
	
	private boolean getBooleanOption(
		final String name,
		final boolean defaultValue)
	{
		String option;
		return XChars.isEmpty(option = this.processingEnv.getOptions().get(name))
			? defaultValue
			: Boolean.parseBoolean(option);
	}
	
	@Override
	public synchronized void init(final ProcessingEnvironment processingEnv)
	{
		super.init(processingEnv);
		
		this.javaLangObjectMethods = processingEnv.getElementUtils()
			.getTypeElement(Object.class.getName())
			.getEnclosedElements().stream()
			.filter(e -> e.getKind() == ElementKind.METHOD)
			.map(ExecutableElement.class::cast)
			.filter(method -> !method.getModifiers().contains(Modifier.STATIC))
			.collect(Collectors.toList());
		
		this.runtimeExceptionType  = processingEnv.getElementUtils()
			.getTypeElement(RuntimeException.class.getName())
			.asType();
	}
	
	@Override
	public boolean process(
		final Set<? extends TypeElement> annotations,
		final RoundEnvironment roundEnv)
	{
		if(roundEnv.processingOver())
		{
			return false;
		}
		
		roundEnv.getRootElements().stream()
			.filter(e -> e.getKind() == ElementKind.INTERFACE)
			.map(TypeElement.class::cast)
			.filter(this::isEntity)
			.forEach(this::generateTypes);
		
		return false;
	}
	
	private boolean isEntity(final TypeElement typeElem)
	{
		return typeElem.getInterfaces().stream()
			.anyMatch(this::isEntity);
	}
	
	private boolean isEntity(final TypeMirror type)
	{
		if(type.getKind() == TypeKind.DECLARED)
		{
			final DeclaredType declaredType = (DeclaredType)type;
			final TypeElement  element      = (TypeElement)declaredType.asElement();
			if(element.getQualifiedName().contentEquals(Entity.class.getName()))
			{
				return true;
			}
			
			return this.isEntity(element);
		}
		
		return false;
	}
	
	private void generateTypes(final TypeElement entityTypeElement)
	{
		final Set<ExecutableElement> potentialMemberMethods = new LinkedHashSet<>();
		this.collectPotentialMemberMethods(entityTypeElement, potentialMemberMethods);
		potentialMemberMethods.forEach(this::validateMemberMethod);
		
		final DeclaredType     entityType  = (DeclaredType)entityTypeElement.asType();
		final List<Member>     members     = potentialMemberMethods.stream()
			.map(element -> new Member(element, this.getTypeInEntity(entityType, element)))
			.collect(Collectors.toList());
		
		final List<SourceFile> sourceFiles = new ArrayList<>(5);
		sourceFiles.add(new DataSourceFile(this.processingEnv, entityTypeElement, members));
		sourceFiles.add(new IdentitySourceFile(this.processingEnv, entityTypeElement, members));
		sourceFiles.add(new CreatorSourceFile(this.processingEnv, entityTypeElement, members));
		if(this.getBooleanOption(OPTION_UPDATER, true))
		{
			sourceFiles.add(new UpdaterSourceFile(this.processingEnv, entityTypeElement, members));
		}
		if(this.getBooleanOption(OPTION_HASHEQUALATOR, true))
		{
			sourceFiles.add(new HashEqualatorSourceFile(this.processingEnv, entityTypeElement, members));
		}
		sourceFiles.forEach(SourceFile::generateType);
	}
	
	private void collectPotentialMemberMethods(
		final TypeElement typeElement,
		final Set<ExecutableElement> members)
	{
		typeElement.getEnclosedElements().stream()
			.filter(e -> e.getKind() == ElementKind.METHOD)
			.map(ExecutableElement.class::cast)
			.filter(method -> this.isPotentialMemberMethod(method, members))
			.forEach(members::add);
		
		typeElement.getInterfaces().stream()
			.filter(type -> type.getKind() == TypeKind.DECLARED)
			.map(DeclaredType.class::cast)
			.map(DeclaredType::asElement)
			.map(TypeElement.class::cast)
			.filter(element -> !element.getQualifiedName().contentEquals(Entity.class.getName()))
			.forEach(element -> this.collectPotentialMemberMethods(element, members));
	}
	
	private boolean isPotentialMemberMethod(
		final ExecutableElement method,
		final Collection<ExecutableElement> methods)
	{
		return !method.isDefault()
			&& !method.getModifiers().contains(Modifier.STATIC)
			&& !this.isOverwritten(method, methods)
			&& !this.overridesObjectMethod(method);
	}
	
	private boolean isOverwritten(
		final ExecutableElement overridden,
		final Collection<ExecutableElement> methods)
	{
		final Elements elements = this.processingEnv.getElementUtils();
		return methods.stream()
			.filter(overrider -> overridden != overrider
				&& (elements.overrides(overrider, overridden, (TypeElement)overrider.getEnclosingElement())
					|| elements.overrides(overridden, overrider, (TypeElement)overridden.getEnclosingElement())))
			.findAny()
			.isPresent();
	}
	
	private boolean overridesObjectMethod(
		final ExecutableElement method)
	{
		final Elements elements = this.processingEnv.getElementUtils();
		return this.javaLangObjectMethods.stream()
			.filter(objectMethod -> elements.overrides(method, objectMethod, (TypeElement)method.getEnclosingElement()))
			.findAny()
			.isPresent();
	}
	
	private void validateMemberMethod(final ExecutableElement method)
	{
		if(method.getReturnType().getKind() == TypeKind.VOID
			|| method.getTypeParameters().size() > 0
			|| method.getParameters().size() > 0
			|| this.containsCheckedException(method.getThrownTypes()))
		{
			throw new EntityException(VarString.New("Invalid entity method: ")
				.add(method.getEnclosingElement()).add('#').add(method)
				.add("; only methods with return type, no type parameters")
				.add(", no parameters and no checked exceptions are supported.").toString());
		}
	}
	
	private boolean containsCheckedException(final List<? extends TypeMirror> exceptionTypes)
	{
		return exceptionTypes.stream().anyMatch(this::isCheckedException);
	}
	
	private boolean isCheckedException(final TypeMirror exceptionType)
	{
		return !this.processingEnv.getTypeUtils().isAssignable(exceptionType, this.runtimeExceptionType);
	}
	
	private TypeMirror getTypeInEntity(
		final DeclaredType entityType,
		final ExecutableElement method)
	{
		final TypeMirror memberType = this.processingEnv.getTypeUtils().asMemberOf(entityType, method);
		return memberType instanceof ExecutableType
			? ((ExecutableType)memberType).getReturnType()
			: memberType;
	}
}
