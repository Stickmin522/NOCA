#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <stddef.h>
extern int32_t noca_build_read(const uint8_t*,uint16_t,uint8_t,uint8_t,uint8_t*,size_t);
extern int32_t noca_response(const uint8_t*,const uint8_t*,size_t,uint8_t*,size_t);
extern int32_t noca_history(const uint8_t*,size_t,uint8_t*,size_t);
extern int32_t noca_balance(const uint8_t*,size_t);
static void fail(JNIEnv*e,const char*m){jclass c=(*e)->FindClass(e,"java/lang/IllegalArgumentException");if(c)(*e)->ThrowNew(e,c,m);}
static jbyteArray bytes(JNIEnv*e,uint8_t*p,int32_t n){if(n<0){fail(e,"Invalid or unsupported FeliCa response");return NULL;}jbyteArray a=(*e)->NewByteArray(e,n);if(a)(*e)->SetByteArrayRegion(e,a,0,n,(jbyte*)p);return a;}
JNIEXPORT jbyteArray JNICALL Java_app_noca_NativeCore_buildRead(JNIEnv*e,jobject o,jbyteArray id,jint service,jint start,jint count){
 (void)o;if(!id||(*e)->GetArrayLength(e,id)!=8||service<0||service>65535||start<0||start>255||count<1||count>12){fail(e,"Invalid read request");return NULL;}
 jbyte p[8];(*e)->GetByteArrayRegion(e,id,0,8,p);uint8_t out[64];int n=noca_build_read((uint8_t*)p,(uint16_t)service,(uint8_t)start,(uint8_t)count,out,sizeof(out));return bytes(e,out,n);
}
JNIEXPORT jbyteArray JNICALL Java_app_noca_NativeCore_readResponse(JNIEnv*e,jobject o,jbyteArray id,jbyteArray response){
 (void)o;if(!id||!response||(*e)->GetArrayLength(e,id)!=8){fail(e,"Invalid card identifier");return NULL;}jsize n=(*e)->GetArrayLength(e,response);if(n>255||n<12){fail(e,"Invalid response length");return NULL;}
 jbyte p[8],data[255];uint8_t out[256];(*e)->GetByteArrayRegion(e,id,0,8,p);(*e)->GetByteArrayRegion(e,response,0,n,data);return bytes(e,out,noca_response((uint8_t*)p,(uint8_t*)data,(size_t)n,out,sizeof(out)));
}
JNIEXPORT jstring JNICALL Java_app_noca_NativeCore_decodeHistory(JNIEnv*e,jobject o,jbyteArray blocks){
 (void)o;if(!blocks){fail(e,"Missing records");return NULL;}jsize n=(*e)->GetArrayLength(e,blocks);if(n>320||n%16){fail(e,"Invalid records");return NULL;}
 jbyte data[320];(*e)->GetByteArrayRegion(e,blocks,0,n,data);uint8_t out[16384];int len=noca_history((uint8_t*)data,(size_t)n,out,sizeof(out)-1);if(len<0){fail(e,"Invalid history record");return NULL;}out[len]=0;return (*e)->NewStringUTF(e,(char*)out);
}
JNIEXPORT jint JNICALL Java_app_noca_NativeCore_attributeBalance(JNIEnv*e,jobject o,jbyteArray block){(void)o;if(!block||(*e)->GetArrayLength(e,block)!=16){fail(e,"Invalid attribute block");return -1;}jbyte b[16];(*e)->GetByteArrayRegion(e,block,0,16,b);return noca_balance((uint8_t*)b,16);}
