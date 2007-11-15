package com.macaufly.gwt.exporter.rebind;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.macaufly.gwt.exporter.client.IExportable;
import com.macaufly.gwt.exporter.client.IExportableImportedStub;

public class ExporterGenerator extends Generator {

	TreeLogger logger;
	GeneratorContext context;
	
	JClassType exportHelperClassType;
	JClassType IExportClassType;
	
	Set exportable;
	
	Set exportMethods;
	Set importMethods;
	
	static final String ARG_PREFIX = "arg";
	static final String SUFFIX_DELEGA = "Delega";
	
	public String generate(TreeLogger logger, GeneratorContext context,
			String typeName) throws UnableToCompleteException {

		init(logger, context, typeName);

		Set iExportablesClass = null;

		try {
			iExportablesClass = getExportableAndfindExportOrImportMethod();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

		genIExportableFactory();
		
		String generatedFullName = genExporterImplement(typeName,iExportablesClass);
		
		return generatedFullName;
	}
	
	private void genIExportableFactory() {
		for(Iterator ite = exportable.iterator();ite.hasNext();){
			genServiceFactory(((Class)ite.next()).getName());
		}
	}

	String genExporterImplement(String typeName,Set iExportablesClass){
		
		JClassType exporterImplClass = context.getTypeOracle().findType(typeName);
		
		Set imports = getImports();

		imports.addAll(iExportablesClass);

		String packageName = exporterImplClass.getPackage().getName();
		
	    String baseName = exporterImplClass.getName().replace('.', '_');
		
		String generatedSimpleName = baseName+"_Impl";
		
		SourceWriter exporterImplSw = getSourceWriter(logger, context, imports, packageName,
				generatedSimpleName);

		String generatedFullName = packageName + "." + generatedSimpleName;
		
		if(exporterImplSw == null )return generatedFullName;
			
		exporterImplSw.println();
		
		genExporterImplMethods(exporterImplClass,exporterImplSw);
		
		exporterImplSw.commit(logger);
		
		return generatedFullName;
	}
	
	void genExporterImplMethods(JClassType exporterImplClass,SourceWriter sw){
		
		for (Iterator ite = importMethods.iterator(); ite.hasNext();) {
			JMethod method = (JMethod) ite.next();

			String returnSimpleName = method.getReturnType()
					.getSimpleSourceName();

			String staticMethodName = returnSimpleName + SUFFIX_FACTORY
					+ ".doImport";

			sw.println("public " + returnSimpleName + " " + method.getName()
					+ "(JavaScriptObject jso){");
			sw.indent();

			sw.println("return " + staticMethodName + "(jso);");
			sw.outdent();
			sw.println("};");
			sw.println();
		}
		for (Iterator ite = exportMethods.iterator(); ite.hasNext();) {

			JMethod method = (JMethod) ite.next();

			String paramSimpleName = method.getParameters()[0].getType()
					.getSimpleSourceName();

			String paramFullName = method.getParameters()[0].getType()
					.getQualifiedSourceName();

			JClassType exportClass = context.getTypeOracle().findType(
					paramFullName);

			writeExportMethod(sw,method, paramSimpleName);
		}
	}
	
	private void init(TreeLogger logger, GeneratorContext context,
			String typeName) {

		this.context = context;

		this.logger = logger;

		exportHelperClassType = context.getTypeOracle().findType(typeName);

		IExportClassType = context.getTypeOracle().findType(
				IExportable.class.getName());

		exportable = new HashSet();

		exportMethods = new HashSet();

		importMethods = new HashSet();

	}

	private void writeDelegaConstructor(SourceWriter sw, String stubClassName,
			String serviceInterface, JClassType stubFather) {
		
		if (stubFather.isInterface() != null) {
			sw.println("public static class " + stubClassName + " implements "
					+ serviceInterface + ",IExportableImportedStub{");
		} else
			sw.println("public static class " + stubClassName + " extends "
					+ serviceInterface + " implements IExportableImportedStub{");

		sw.indent();
		sw.println();
		sw.println("private JavaScriptObject jso;");
		sw.println();
		sw.println("public " + stubClassName
				+ " (JavaScriptObject jso){this.jso = jso;}");
		
		sw.println();
		sw.println("public JavaScriptObject getJavaScriptStub(){");
		sw.indent();
		sw.println("return jso;");
		sw.outdent();
		sw.println("}");
		sw.println();
	}

	private void writeDelegaMethod(SourceWriter sw, String stubClassName,
			JMethod method, String serviceInterface, boolean isNeedFindService,JClassType exClass) {

		String pkgName = exClass.getPackage().getName();
		
		String returnSimpleName = method.getReturnType().getSimpleSourceName();

		String returnFullName = method.getReturnType().getQualifiedSourceName();

		String defineParamString = argString(method.getParameters())[0]; // (String arg0,String arg1)

		String invokeParamString = argString(method.getParameters())[1]; // (arg0,arg1)

		String methodName = method.getName();

		sw.println("public native " + returnSimpleName + " " + methodName
				+ defineParamString + "/*-{");
		sw.indent();

		for (int l = 0; l < method.getParameters().length; l++) {
			String paramFullName = method.getParameters()[l].getType()
					.getQualifiedSourceName();
			String exp = generateIfNeedExport(paramFullName, ARG_PREFIX + l);
			if (!exp.equals(""))
				sw.println(ARG_PREFIX + l + " = " + exp);
		}

		sw.println("var ret ; ");

		sw.println("ret = this.@" + pkgName + "." + serviceInterface
				+ SUFFIX_FACTORY + "." + stubClassName + "::jso." + methodName
				+ invokeParamString + ";");

		sw.println();

		if (!returnSimpleName.equals("void")) {
			if (isExportable(returnFullName)) {
				String pkg = context.getTypeOracle().findType(method.getReturnType().getQualifiedSourceName()).getPackage().getName();
				sw
						.println("ret = @"
								+ pkg
								+ "."
								+ returnSimpleName
								+ SUFFIX_FACTORY
								+ "::doImport"
								+ "(Lcom/google/gwt/core/client/JavaScriptObject;)(ret)");
			}
			sw.println("return ret;");
		}

		sw.outdent();
		sw.println("}-*/;");
		sw.println();
	}

	/**
	 * public static class IFace1Stub implements IFace1 {
	 * 
	 * private JavaScriptObject jso;
	 * 
	 * public IFace1Stub(JavaScriptObject jso) { this.jso = jso; }
	 * 
	 * public native IFace2 method1(String arg0, IFace1 arg1, IFace2 arg2)
	 * }                                                                                                             }-* /; }
	 */
	private void generateDoDelega(SourceWriter sw, JClassType exClass,
			boolean isNeedFindService) {

		String stubClassName = exClass.getSimpleSourceName() + SUFFIX_DELEGA;

		String serviceInterface = exClass.getSimpleSourceName();

		writeDelegaConstructor(sw, stubClassName, serviceInterface, exClass);

		JMethod[] stubMethod = exClass.getMethods();

		for (int i = 0; i < stubMethod.length; i++) {
			writeDelegaMethod(sw, stubClassName, stubMethod[i], serviceInterface,
					isNeedFindService,exClass);
		}

		sw.outdent();
		sw.println("}");
		sw.println();
	}

	private void writeExportStaticMethod(SourceWriter sw,
			String staticMethodName, String paramSimpleName,
			String paramFullName, JClassType exportClass) {
		sw.println("public static native JavaScriptObject " + staticMethodName
				+ "(" + paramSimpleName + " jo)/*-{");

		sw.indent();
		sw.println("var returnObject = {");

		sw.indent();

		JMethod exportClassMethods[] = exportClass.getMethods();

		for (int i = 0; i < exportClassMethods.length; i++) {
			writeExportStaticMethodReturnInner(sw, exportClassMethods[i],
					paramFullName, !(i < exportClassMethods.length - 1));
		}
		sw.outdent();
		sw.println("};");

		sw.println("return returnObject;");

		sw.outdent();

		sw.println("}-*/;");
		sw.println();
	}

	/**
	 * @param jsMethod
	 * @param invokeClassName
	 * @param end
	 */
	private void writeExportStaticMethodReturnInner(SourceWriter sw,
			JMethod jsMethod, String invokeClassName, boolean end) {

		sw.println(jsMethod.getName() + " : function"
				+ argString(jsMethod.getParameters())[1] + "{");
		sw.indent();

		generateIfNeedImport(sw, jsMethod.getParameters());

		String[] argParamString = argJNISSignature(jsMethod.getParameters());

		sw.println("var ret = " + "jo.@" + invokeClassName + "::"
				+ jsMethod.getName() + argParamString[0] + argParamString[1]
				+ ";");

		if (!jsMethod.getReturnType().getSimpleSourceName().equals("void")) {
			String exp = generateIfNeedExport(jsMethod.getReturnType()
					.getQualifiedSourceName(), "ret");
			if (!exp.equals(""))
				sw.println("return " + exp);
			else
				sw.println("return ret;");
		}

		sw.outdent();

		sw.println("}" + (end ? "" : ","));
	}

	/**
	 * like this: public JavaScriptObject doExport(IFace1 jo){<br>
	 * return doExport0(jo); }
	 */
	private void writeExportMethodForFactory(SourceWriter sw, String paramName,
			String staticProxyMethod) {

		sw.println("public static JavaScriptObject doExport(" + paramName
				+ " jo){");
		sw.indent();
		sw
				.println("if(jo instanceof IExportableImportedStub){IExportableImportedStub s = (IExportableImportedStub)jo; return s.getJavaScriptStub();}");
		sw.println("return " + staticProxyMethod + "(jo);");
		sw.outdent();
		sw.println("}");
		sw.println();
	}

	/**
	 * like this: public JavaScriptObject doExport(IFace1 jo){<br>
	 * return doExport0(jo); }
	 */
	private void writeExportMethod(SourceWriter sw,JMethod method, String paramName) {

		sw.println("public JavaScriptObject " + method.getName() + "("+ paramName + " jo){");
		sw.indent();
		sw.println("return " + paramName + SUFFIX_FACTORY + ".doExport(jo);");
		sw.outdent();
		sw.println("}");
		sw.println();
	}

	/**
	 * 
	 * @param typeName :
	 *            arg 's fullName
	 * @param arg
	 * @return String = "arg1 =
	 * @classFullName::doExport(Ljava/iexportable;)(arg1);"
	 */
	private String generateIfNeedExport(String typeName, String arg) {
		String returnStr = "";

		if (isExportable(typeName)) {
			JClassType ct = context.getTypeOracle().findType(typeName);
			
			String pkgName = ct.getPackage().getName();
			
			returnStr += "@" + pkgName + "." + ct.getSimpleSourceName()
					+ SUFFIX_FACTORY + "::doExport(" + ct.getJNISignature()
					+ ")(" + arg + ");";

		}
		return returnStr;
	}

	/**
	 * @param params
	 * @return String[2] like this :
	 *         {"(Ljava/lang/String;Ljava/lang/String;)","(arg0,arg1)"}
	 */
	protected String[] argJNISSignature(JParameter params[]) {
		String reference = "(";
		String reference2 = "(";
		for (int i = 0; i < params.length; i++) {
			reference += params[i].getType().getJNISignature();
			reference2 += ARG_PREFIX + i;
			if (i < params.length - 1) {
				reference2 += (", ");
			}
		}
		reference += ")";
		reference2 += ")";
		String[] rt = { reference, reference2 };
		return rt;
	}

	/**
	 * sw will println this:<br>
	 * arg1 = @ test.MyExportable::doImportIFace1(Lcom/google/gwt/core/client/JavaScriptObject;)(arg1);
	 * @param parameters :
	 *            exportMethod's paramters
	 */
	private void generateIfNeedImport(SourceWriter sw, JParameter[] parameters) {
		for (int i = 0; i < parameters.length; i++) {
			
			JType paramType =parameters[i].getType();

			if (!isExportable(paramType.getQualifiedSourceName()))
				continue;
			
			JClassType paramCType = context.getTypeOracle().findType(paramType.getQualifiedSourceName());

			String pkgName = paramCType.getPackage().getName();
			
			sw.println(ARG_PREFIX + i + " = " + "@" + pkgName + "."
					+ paramType.getSimpleSourceName() + SUFFIX_FACTORY
					+ "::doImport"
					+ "(Lcom/google/gwt/core/client/JavaScriptObject;)("
					+ ARG_PREFIX + i + ");");
		}
	}

	/**
	 * @param JParameter
	 * @return String[2] like this : {"(String arg0,Object arg1)","(arg0,arg1)"}
	 */
	protected String[] argString(JParameter[] params) {
		String reference = "(";
		String reference2 = "(";
		for (int i = 0; i < params.length; i++) {
			reference += params[i].getType().getSimpleSourceName() + " "
					+ ARG_PREFIX + i;
			reference2 += ARG_PREFIX + i;
			if (i < params.length - 1) {
				reference += (", ");
				reference2 += (", ");
			}
		}
		String arg[] = { reference + ")", reference2 + ")" };
		return arg;
	}

	private boolean isJavaScriptObject(String simpleClassName) {
		return JavaScriptObject.class.getName().equals(simpleClassName);
	}

	private Set getImports() {

		Set imports = new HashSet();

		imports.add(JavaScriptObject.class);
		imports.add(Element.class);
		imports.add(GWT.class);
		imports.add(IExportableImportedStub.class);

		return imports;
	}

	/**
	 * iterator the exportHelper methods,find which method need export,whick
	 * need import<br>
	 * and return the class which implement IExportale
	 * 
	 * @return Set : IFace1.class,IFace2.class
	 * @throws ClassNotFoundException
	 */
	private Set getExportableAndfindExportOrImportMethod()
			throws ClassNotFoundException {

		JMethod[] methods = exportHelperClassType.getMethods();

		for (int i = 0; i < methods.length; i++) {

			JMethod method = methods[i];

			JParameter params[] = method.getParameters();

			if (params.length > 1)
				throw new RuntimeException(
						"param's lenght from export method must be one");

			String paramClassName = params[0].getType()
					.getQualifiedSourceName();

			String returnClassName = method.getReturnType()
					.getQualifiedSourceName();

			if (isExportable(paramClassName)
					&& isJavaScriptObject(returnClassName))
				// if(isJavaScriptObject(returnClassName))
				exportMethods.add(methods[i]);

			if (isJavaScriptObject(paramClassName)
					&& isExportable(returnClassName))
				// if(isJavaScriptObject(paramClassName))
				importMethods.add(methods[i]);

			if (isExportable(paramClassName))
				exportable.add(Class.forName(paramClassName));

			if (isExportable(returnClassName))
				exportable.add(Class.forName(returnClassName));

		}

		Set needImportClass = new HashSet();
		needImportClass.addAll(exportable);

		return needImportClass;
	}

	private void genExportAtServiceInner(JClassType serviceClass,
			Set needImportClass, Set exportableTemp, Set exportable) {

		JMethod[] jMethods = serviceClass.getMethods();

		for (int l = 0; l < jMethods.length; l++) {
			JMethod mt = jMethods[l];

			if (!"void".equals(mt.getReturnType().getSimpleSourceName())) {

				String cName = mt.getReturnType().getQualifiedSourceName();

				addImportAndExportable(cName, needImportClass, exportableTemp,
						exportable);
			}

			JParameter[] params = mt.getParameters();
			for (int i = 0; i < params.length; i++) {
				Class addClass = null;

				String cName = params[i].getType().getQualifiedSourceName();

				addImportAndExportable(cName, needImportClass, exportableTemp,
						exportable);
			}
		}
	}

	private void addImportAndExportable(String cName, Set needImportClass,
			Set exportableTemp, Set exportable) {
		Class addClass = null;

		try {
			addClass = Class.forName(cName);
		} catch (ClassNotFoundException e) {
		}

		if (exportable.contains(addClass) || needImportClass.contains(addClass)
				|| exportableTemp.contains(addClass))
			return;

		if (isExportable(cName)) {
			if (addClass != null) {
				needImportClass.add(addClass);
				exportableTemp.add(addClass);
			}
		}
	}

	private boolean isExportable(String className) {
		JClassType iex2 = context.getTypeOracle().findType(className);
		return IExportClassType.isAssignableFrom(iex2);
	}

	private SourceWriter getSourceWriter(TreeLogger logger,
			GeneratorContext ctx, Set imports, String packageName,
			String generatedSimpleName) {

		// get writer
		PrintWriter printWriter = ctx.tryCreate(logger, packageName,
				generatedSimpleName);
		if (printWriter == null) {
			return null;
		}

		// get composer factory
		ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(
				packageName, generatedSimpleName);

		// add imports required
		for (Iterator iter = imports.iterator(); iter.hasNext();) {
			Class typeToImport = (Class) iter.next();
			composerFactory.addImport(typeToImport.getPackage().getName()+".*");
//			composerFactory.addImport(typeToImport.getName());
		}

		composerFactory.addImplementedInterface(exportHelperClassType
				.getQualifiedSourceName());

		// create source writer
		SourceWriter sw = composerFactory.createSourceWriter(ctx, printWriter);
		return sw;
	}

	/**
	 * write service factory: public class Afactory{ static native
	 * JavaScriptObject doExport(A a)/-{ return { method1 = function(){
	 * jo.@asldfjals;jdfkasdj::mehtod1()(); } } }-/;
	 * 
	 * static A doImport(JSO){ return new AProxy(JSO); }
	 * 
	 * class AProxy implement A{ } }
	 */
	static final String SUFFIX_FACTORY = "_Factory";

	void genServiceFactory(String exportClassName) {

		JClassType exportClass = context.getTypeOracle().findType(
				exportClassName);

		Set exports = findFactoryImports(exportClass);
		
		Set imports = getImports();
		imports.addAll(exports);
		
		SourceWriter facotrySw = createFacotrySourceWrite(exportClass,imports);

		if (facotrySw == null)
			return;

		generateDoFactoryImport(facotrySw, exportClass);

		genFactoryExport(facotrySw, exportClass,exports);
		
		facotrySw.commit(logger);
	}
	
	private void genFactoryExport(SourceWriter facotrySw,JClassType exportClass,Set exportable){

		String staticMethodName = "doExport_";
		String paramSimpleName = exportClass.getSimpleSourceName();
		String paramFullName = exportClass.getQualifiedSourceName();
		
		writeExportMethodForFactory(facotrySw, paramSimpleName,
				staticMethodName);

		writeExportStaticMethod(facotrySw, staticMethodName, paramSimpleName,
				paramFullName, exportClass);

		generateDoDelega(facotrySw, exportClass, false);
		
		Set hasExportClass = new HashSet();
		try {
			hasExportClass.add(Class.forName(paramFullName));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	
		Set needExportClass = new HashSet();
		needExportClass.addAll(exportable);
		needExportClass.removeAll(hasExportClass);
	
		for (Iterator ite = needExportClass.iterator(); ite.hasNext();) {
	
			JClassType exportC = context.getTypeOracle().findType(
					((Class) ite.next()).getName());
	
			String pFullName = exportC.getQualifiedSourceName();
	
			genServiceFactory(pFullName);
		}
	}

	private void generateDoFactoryImport(SourceWriter sw, JClassType exportClass) {

		String returnSimpleName = exportClass.getSimpleSourceName();

		String staticMethodName = "doImport";
		sw.println();
		sw.println("public static " + returnSimpleName + " " + staticMethodName
				+ "(JavaScriptObject jso){");
		sw.indent();
		sw.println("return new " + returnSimpleName + SUFFIX_DELEGA + "(jso);");
		sw.outdent();
		sw.println("};");
		sw.println();
	}

	SourceWriter createFacotrySourceWrite(JClassType exportClass,Set imports) {
		
		String pkgName = exportClass.getPackage().getName(); //创建到各自package

		String genSimpleName = exportClass.getSimpleSourceName()
				+ SUFFIX_FACTORY;

		PrintWriter pw = context.tryCreate(logger, pkgName, genSimpleName);

		if (pw == null)
			return null;

		ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(
				pkgName, genSimpleName);

		for (Iterator ite = imports.iterator(); ite
				.hasNext();) {
			Class typeToImport = (Class) ite.next();
			composerFactory.addImport(typeToImport.getPackage().getName()+".*");
		}

		SourceWriter factorySw = composerFactory
				.createSourceWriter(context, pw);
		return factorySw;
	}

	Set findFactoryImports(JClassType exportClass) {
		Set needImportClass = new HashSet();
		Set exportableTemp = new HashSet();
		Set exportable = new HashSet();

		try {
			exportable.add(Class.forName(exportClass.getQualifiedSourceName()));
		} catch (ClassNotFoundException e) {
		}
		
		genExportAtServiceInner(exportClass, needImportClass, exportableTemp,
				exportable);
		
		return needImportClass;
	}
}
