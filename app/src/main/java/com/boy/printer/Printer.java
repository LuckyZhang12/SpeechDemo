package com.boy.printer;

import android.graphics.Bitmap;

import java.io.IOException;

/**
 * Created by yankun on 15-11-20.
 */
public interface Printer {

	void destroyPrinter();

	void printMsg(String msg) throws IOException;

    void printBitmap(Bitmap bitmap) throws IOException;

}
