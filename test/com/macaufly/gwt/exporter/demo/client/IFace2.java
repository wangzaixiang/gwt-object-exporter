package com.macaufly.gwt.exporter.demo.client;

import com.macaufly.gwt.exporter.client.IExportable;


/**
 * @author jiangyongyuan
 */
public interface IFace2 extends IExportable{
	void syshello();
	IFace2 doIFace1(IFace1 face1);
}
