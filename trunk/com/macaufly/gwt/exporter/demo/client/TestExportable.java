package com.macaufly.gwt.exporter.demo.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

public class TestExportable implements EntryPoint {

	static class Face2 implements IFace2 {
		public void syshello() {
			System.out.println("hello");
		}

		public IFace2 doIFace1(IFace1 face1) {
			return face1.method1("face2 doIFace1", face1, this);
		}
	}

	static class Face1 implements IFace1 {
		public IFace2 method1(String s1, IFace1 f1, IFace2 f2) {
			System.out.println("s1 = " + s1);
			return f2;
		}

		public IFace1 method2(String s1, IFace1 f1, IFace2 f2) {
			System.out.println(s1);
			return f1;
		}

		public void method3(Bean bean) {
			// TODO Auto-generated method stub
			
		}
	}

	/**
	 * the generated MyExportableHelper will look like ----------------- <code>
	
	JavaScriptObject doExport(IFace1 iface1){

		if(iface1 instanceof IExportableImportedStub){
			IExportableImportedStub s = iface1;
			return s.getJavascriptStub;
		}
		else return
		{
			method1: function(s, f1, f2){ // f1, f2 is now a JavaScript exchange 
				var if1 = doImportIface1(f1); // import the JavaScriptStub as interface
				var if2 = doImportIface2(f2);
				var res = iface1.@IFace1::method1(..)(s, xf1, xf2);
				return doExportIface2(res);
			}
		};
	}

	doImportIFace1(js){
		return new IFace1Stub(js)
	}

	public class IFace1Stub implements IFace1, IExportableImportedStub {

		private JavaScriptObject jsExchange;
		
		public IFace1Stub(JavascriptObject jso){
			this.jsExchange = jso;
		}

		Iface2 method1(String s1, IFace f1, IFace f2) /*-{
			var xf1 = doExportIFace1(f1);
			var xf2 = doExportIFace2(f2);
		
			var xreturn = jsExchange.method1(s1, xf1, xf2);
			return doImportIface2(xreturn);
		}-* /

	}
	</code>
	 */

	public void onModuleLoad() {

		Face2 face2 = new Face2();
		Face1 face1 = new Face1();

		MyExportableHelper helper = (MyExportableHelper) GWT.create(MyExportableHelper.class);
		
		JavaScriptObject jso = helper.doExport(face1);
		
		IFace1 iface1 = helper.doImportIFace1(jso);	//默认为已经doExport的jso
		
		IFace2 iface2 = iface1.method1("123", face1, face2);
		iface2.syshello();
		iface2.doIFace1(iface1);
		
		IFace1 iface12 = iface1.method2("456", face1, face2);
		iface12.method1("123", face1, face2);
		
	}
}
