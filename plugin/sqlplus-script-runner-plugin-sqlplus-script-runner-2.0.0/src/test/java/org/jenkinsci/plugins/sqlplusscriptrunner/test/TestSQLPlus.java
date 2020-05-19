package org.jenkinsci.plugins.sqlplusscriptrunner.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.jenkinsci.plugins.sqlplusscriptrunner.ScriptType;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TestSQLPlus {

	private static final String SCRIPT_SQL = "temp-script-15712810217574695150744131466792.sql.1";
	private static final String ORACLE_PASSWORD = "WO6UVD4wQoVqssAS";
	private static final String ORACLE_USER = "bi_wanghf";
	static final String ORACLE_HOME = "/Users/wanghf/Documents/soft/oracle1";
	static final String ORACLE_INSTANCE = "NBIDB";
	static final String WORK_DIR = System.getProperty("/Users/wanghf/jenkins-master/workspace/test_ods_8");

	@Test
	public void testVersion() throws IOException,InterruptedException {

		String detectedVersion = ExternalProgramUtil.getVersion(WORK_DIR,ORACLE_HOME);

		System.out.println("SQLPlus detected version = " + detectedVersion);

		assertTrue(detectedVersion.contains("SQL*Plus: Release 12.1.0.1.0 Production"));
	}

	@Test
	public void testUserDefinedScriptFile() throws IOException,InterruptedException {

		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource(SCRIPT_SQL).getFile());

		String output = ExternalProgramUtil.run(ORACLE_USER,ORACLE_PASSWORD,ORACLE_INSTANCE,file.getCanonicalPath(),WORK_DIR,ORACLE_HOME,ScriptType.userDefined.name());

		System.out.println("output = " + output);

		assertTrue(output.contains("BANNER"));
	}

	@Test
	public void testRunningScriptFile() throws IOException,InterruptedException {

		String output = ExternalProgramUtil.run(ORACLE_USER,ORACLE_PASSWORD,ORACLE_INSTANCE,"temp-script-15712810217574695150744131466792.sql.1",WORK_DIR,ORACLE_HOME,ScriptType.file.name());

		System.out.println("output = " + output);

		assertTrue(output.contains("BANNER"));
	}

}
