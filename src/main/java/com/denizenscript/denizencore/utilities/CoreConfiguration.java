package com.denizenscript.denizencore.utilities;

import java.nio.charset.CharsetDecoder;

@ReflectionRefuse
public class CoreConfiguration {

    public static long deprecationWarningRate = 10000;

    public static boolean futureWarningsEnabled = false;

    public static volatile boolean debugLoadingInfo = false;

    public static boolean debugVerbose = false, debugUltraVerbose = false, debugExtraInfo = false,
            debugOverride = false, debugStackTraces = true,
            debugScriptBuilder = false, debugShowSources = false, debugShouldTrim = true,
            debugRecordingAllowed = false;

    public static int debugLimitPerTick = 5000, debugTrimLength = 1024, debugLineLength = 300;

    public static boolean allowWebget = false, allowSQL = false, allowRedis = false, allowMongo = false, allowLog = false, allowFileCopy = false, allowWebserver = false, allowFileRead = false, allowFileWrite = false;

    public static boolean allowConsoleRedirection = false, allowRestrictedActions = false, allowStrangeFileSaves = false;

    public static boolean tagTimeoutWhenSilent = false, tagTimeoutUnsafe = false;

    public static int tagTimeout = 0;

    public static boolean defaultDebugMode = true;

    public static int whileMaxLoops = 10000;

    public static double scriptQueueSpeed = 0;

    public static volatile CharsetDecoder scriptEncoding;

    public static boolean skipAllFlagCleanings = false;

    public static String webserverRoot = "webroot/", filePathLimit = "data/";

    public static boolean verifyThreadMatches;

    public static boolean queueIdPrefix = true, queueIdNumeric = true, queueIdWords = true;

    public static boolean listFlagsAllowed = false;

    public static boolean allowReflectionFieldReads = false, allowReflectedCoreMethods = false, allowReflectionSet = false, allowReflectionSetPrivate = false, allowReflectionSetFinal = false;

    public static boolean shouldShowDebug = true, shouldRecordDebug = false;

    public static String debugPrefix = "";
}
