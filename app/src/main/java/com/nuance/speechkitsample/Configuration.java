package com.nuance.speechkitsample;

import android.net.Uri;

import com.nuance.speechkit.PcmFormat;

/**
 * All Nuance Developers configuration parameters can be set here.
 *
 * Copyright (c) 2015 Nuance Communications. All rights reserved.
 */
public class Configuration {

    //All fields are required.
    //Your credentials can be found in your Nuance Developers portal, under "Manage My Apps".
    public static final String APP_KEY = "4a1fa92317d24ac06bf723ed70d36bae1292e68f14586806e26b9203c7a66a11c51335dce57f9c5177bf3ee793a995a53b7b3450d39c56b508e8b8cf8f22e154";
    public static final String APP_ID = "NMDPTRIAL_1007650997_qq_com20160730022953";
    public static final String SERVER_HOST = "sslsandbox.nmdp.nuancemobility.net";
    public static final String SERVER_PORT = "443";

    public static final Uri SERVER_URI = Uri.parse("nmsps://" + APP_ID + "@" + SERVER_HOST + ":" + SERVER_PORT);

    //Only needed if using NLU
    public static final String CONTEXT_TAG = "!NLU_CONTEXT_TAG!";

    public static final PcmFormat PCM_FORMAT = new PcmFormat(PcmFormat.SampleFormat.SignedLinear16, 16000, 1);
}

