package com.macaufly.gwt.exporter.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Module1 ( RealObject implements Interface1 ) Module2 ( Interface1Stub
 * implements Interface1 }
 * 
 * when we need pass realObject to module2, we first export it as an
 * exchangeable Javascript object.
 * 
 * 
 */
public interface IExportableImportedStub {

	/**
	 * the exchange javascript object using well-spelled name which delegate
	 * each call to the orignal function call.
	 */
	public JavaScriptObject getJavaScriptStub();

	/**
	 * the realy object which implements the interface
	 */
//	public JavaScriptObject getOriginalImplementation();

}
