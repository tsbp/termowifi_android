package com.voodoo.termowifi;

/**
 * Created by Voodoo on 08.06.2016.
 */
public class Protocol {

    public static final byte OK_ANS	=			(byte)(0xAA);
    public static final byte BAD_ANS	=		(byte)(0xEE);

    public static final byte BROADCAST_DATA	=			(0x10);
    public static final byte HARDWARE_CFG		=		(0x11);

    public static final byte PLOT_DATA			=		(0x20);
    public static final byte PLOT_DATA_ANS		=		(0x21);

    public static final byte READ_WEEK_CONFIGS	=		(0x30);
    public static final byte READ_WEEK_CONFIGS_ANS	=	(0x31);
    public static final byte SAVE_WEEK_CONFIGS	=		(0x32);

    public static final byte READ_DAY_CONFIGS	=		(0x33);
    public static final byte READ_DAY_CONFIGS_ANS	=	(0x34);
    public static final byte SAVE_DAY_CONFIGS	=		(0x35);

    public static final byte READ_USTANOVKI			=		(0x36);
    public static final byte READ_USTANOVKI_ANS		=		(0x37);
    public static final byte SAVE_USTANOVKI			=		(0x38);


    public static final byte ESP8266_SEARCH = 0;
    public static final byte ESP8266_DATA_RECEIVE = 1;
    public static final byte ESP8266_CONFIG = 3;
    public static final byte ESP8266_LANSET = 4;
    public static final byte ESP8266_UST = 5;
    public static byte mode = 0;
}
