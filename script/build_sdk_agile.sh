#!/bin/bash
export LANG=""
echo "start build sdk!"
sdkVersion="v1_0_0"
libName="BaiduTrace_BatchUpload_SDK_${sdkVersion}_Lib"
echo "build sdk version = ${sdkVersion}"
echo "localpath = $PWD"
echo "javahome = $JAVA_HOME_1_6"

PACKAGE="com/baidu/trace"

echo "CREATE DIRS"
mkdir temp
mkdir outputProguard

cd output/
cd ..

echo "Build release jar"
echo "Compile java source codes"
$JAVA_HOME_1_6/bin/javac  -encoding UTF-8  -sourcepath  src/main/java  -d temp/ -g:none -target 1.6 src/main/java/${PACKAGE}/*.java  src/main/java/${PACKAGE}/core/*.java src/main/java/${PACKAGE}/api/*/*.java src/main/java/${PACKAGE}/model/*.java src/main/java/${PACKAGE}/util/*.java
cd temp/
echo "Generate release jar"
$JAVA_HOME_1_6/bin/jar -cvf BaiduTraceSDK_${sdkVersion}.jar ${PACKAGE}/*.class ${PACKAGE}/api/*/*.class ${PACKAGE}/model/*.class ${PACKAGE}/util/*.class
cd ..
cp temp/BaiduTraceSDK_${sdkVersion}.jar outputProguard/BaiduTraceSDK_NoProguard_${sdkVersion}.jar

echo "Proguard jar"
$JAVA_HOME_1_6/bin/java -jar proguard.jar -injars outputProguard/BaiduTraceSDK_NoProguard_${sdkVersion}_Debug.jar  -outjars outputProguard/BaiduTraceSDK_${sdkVersion}_Debug.jar -printmapping outputProguard/proguard_Debug.map @proguard_lbs.txt
$JAVA_HOME_1_6/bin/java -jar proguard.jar -injars outputProguard/BaiduTraceSDK_NoProguard_${sdkVersion}.jar  -outjars outputProguard/BaiduTraceSDK_${sdkVersion}.jar -printmapping outputProguard/proguard.map @proguard_lbs.txt

cp outputProguard/BaiduTraceSDK_${sdkVersion}.jar output/BaiduTraceSDK_${sdkVersion}.jar
cp outputProguard/proguard_Debug.map output/BaiduTraceSDK_${sdkVersion}_proguard_Debug.map
cp outputProguard/proguard.map output/BaiduTraceSDK_${sdkVersion}_proguard.map
cp output_local/BaiduTraceSDK_${sdkVersion}_Docs.zip output/BaiduTraceSDK_${sdkVersion}_Docs.zip


cd output

zip -g ${libName}.zip BaiduTraceSDK_${sdkVersion}.jar
zip -g ${libName}.zip BaiduTraceSDK_${sdkVersion}_Docs.zip
rm BaiduTraceSDK_${sdkVersion}.jar
rm BaiduTraceSDK_${sdkVersion}_Docs.zip
cd ..

echo "Clean Temp"
rm -r temp
rm -r outputProguard
echo "done!"