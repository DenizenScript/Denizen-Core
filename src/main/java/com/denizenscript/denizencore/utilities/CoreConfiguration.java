package com.denizenscript.denizencore.utilities;

import java.nio.charset.CharsetDecoder;

public class CoreConfiguration {

    public static long deprecationWarningRate = 10000;

    public static boolean futureWarningsEnabled = false;

    public static boolean debugVerbose = false, debugLoadingInfo = false;

    public static boolean allowWebget = false, allowSQL = false, allowRedis = false, allowLog = false, allowFileCopy = false, allowWebserver = false;

    public static boolean allowConsoleRedirection = false, allowRestrictedActions = false, allowStrangeFileSaves = false;

    public static boolean tagTimeoutWhenSilent = false, tagTimeoutUnsafe = false;

    public static int tagTimeout = 0;

    public static boolean defaultDebugMode = true;

    public static int whileMaxLoops = 10000;

    public static double scriptQueueSpeed = 0;

    public static CharsetDecoder scriptEncoding;

    public static boolean skipAllFlagCleanings = false;

    public static String webserverRoot = "webroot/";
}
