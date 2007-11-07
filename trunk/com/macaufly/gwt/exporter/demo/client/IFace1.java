package com.macaufly.gwt.exporter.demo.client;

import com.macaufly.gwt.exporter.client.IExportable;

/**
 * @author jiangyongyuan
 */
public interface IFace1  extends IExportable{
	IFace2 method1(String s1, IFace1 f1, IFace2 f2);
	IFace1 method2(String s1, IFace1 f1, IFace2 f2);
	void method3(Bean bean);
}
