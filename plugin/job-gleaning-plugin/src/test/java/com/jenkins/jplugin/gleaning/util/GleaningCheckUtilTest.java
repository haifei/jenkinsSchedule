package com.jenkins.jplugin.gleaning.util;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * @author wanghf on 2019/7/25
 * @desc
 */
public class GleaningCheckUtilTest {

    @Test
    public void testGetLossContext(){
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd");
        try {
            Map<Long, String> lossContext = GleaningCheckUtil.getLossContext("24 9 * * *", simpleDateFormat.parse("2019-07-20"));
            int i=0;
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
