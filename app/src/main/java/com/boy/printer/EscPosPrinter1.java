package com.boy.printer;

import android.graphics.Bitmap;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by yankun on 15-11-20.
 */
public class EscPosPrinter1 implements Printer{

	PrintWriter socketWriter;

	public EscPosPrinter1(String ip,int port){

	}

	@Override
	public void destroyPrinter() {

	}

	@Override
	public void printMsg(String msg) throws IOException {

	}

    @Override
    public void printBitmap(Bitmap bitmap) throws IOException {

    }
}
