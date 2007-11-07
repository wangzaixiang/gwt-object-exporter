package com.macaufly.gwt.exporter.demo.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.macaufly.gwt.exporter.client.IExporter;

/**
 * 
 * doExport导出jso，并设置$wnd.parent.IFace1 = jso;
 * doImportIFace1(null)	,如果传入jso为null,默认获得$wnd.parent.IFace1;
 * 
 * @author jiangyongyuan
 */
public interface MyExportableHelper extends IExporter {

	public JavaScriptObject doExport(IFace1 iface1);

	public JavaScriptObject doExport(IFace2 iface1);

	public IFace1 doImportIFace1(JavaScriptObject jso);

	public IFace2 doImportIFace2(JavaScriptObject jso);
	
}
