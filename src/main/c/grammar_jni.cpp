/**
 * grammar_jni.cpp
 *
 *  Created on: May 20, 2015
 *      Author: asaparov
 */

#include "edu_cmu_ml_rtw_micro_hdp_HDPParser.h"

#include <stdlib.h>
#include <string.h>

extern "C" {
void run_console(FILE* input, const char* prompt);
void set_memory_limit(long unsigned int new_limit);
void set_data_path(const char* new_path);
char* parse_sentence(const char* sentence, const char* root_nonterminal, unsigned int k, double& probability);
}

JNIEXPORT jboolean JNICALL Java_edu_cmu_ml_rtw_micro_hdp_HDPParser_initialize(
		JNIEnv* env, jobject obj, jstring init_script, jstring hdp_directory, jint memory_lim)
{
	set_memory_limit(memory_lim);

	const char* data_path = env->GetStringUTFChars(hdp_directory, 0);
	if (data_path == NULL) {
		fprintf(stderr, "(JNI) initialize ERROR: Unable to retrieve Java string 'hdp_directory'.\n");
		return false;
	}
	set_data_path(data_path);

	const char* commands = env->GetStringUTFChars(init_script, 0);
	if (commands == NULL) {
		fprintf(stderr, "(JNI) initialize ERROR: Unable to retrieve Java string 'init_script'.\n");
		env->ReleaseStringUTFChars(hdp_directory, data_path);
		return false;
	}
	FILE* init_stream = fmemopen((char*) commands, strlen(commands) + 1, "r");
	if (init_stream == NULL) {
		fprintf(stderr, "(JNI) initialize ERROR: Unable to open in-memory stream.\n");
		env->ReleaseStringUTFChars(hdp_directory, data_path);
		env->ReleaseStringUTFChars(init_script, commands);
		return false;
	}
	run_console(init_stream, "");
	fclose(init_stream);
	env->ReleaseStringUTFChars(init_script, commands);

	return true;
}

JNIEXPORT jobject JNICALL Java_edu_cmu_ml_rtw_micro_hdp_HDPParser_annotate(
		JNIEnv* env, jobject obj, jstring sentence, jstring root_nonterminal, jint k)
{
	const char* sentence_str = env->GetStringUTFChars(sentence, 0);
	if (sentence_str == NULL) {
		fprintf(stderr, "(JNI) annotate ERROR: Unable to retrieve Java string 'sentence'.\n");
		return NULL;
	}

	const char* nonterminal = env->GetStringUTFChars(root_nonterminal, 0);
	if (nonterminal == NULL) {
		fprintf(stderr, "(JNI) annotate ERROR: Unable to retrieve Java string 'root_nonterminal'.\n");
		env->ReleaseStringUTFChars(sentence, sentence_str);
		return NULL;
	}

	double probability = 0.0;
	char* result = parse_sentence(sentence_str, nonterminal, k, probability);
	if (result == NULL) {
		env->ReleaseStringUTFChars(sentence, sentence_str);
		env->ReleaseStringUTFChars(root_nonterminal, nonterminal);
		return NULL;
	}
	jstring result_str = env->NewStringUTF(result);
	jclass double_class = env->FindClass("java/lang/Double");
	jmethodID double_constructor = env->GetMethodID(double_class, "<init>", "(D)V");
	jobject probability_obj = env->NewObject(double_class, double_constructor, probability);

	jclass pair_class = env->FindClass("edu/cmu/ml/rtw/generic/util/Pair");
	jmethodID pair_constructor = env->GetMethodID(pair_class, "<init>", "(Ljava/lang/Object;Ljava/lang/Object;)V");
	jobject pair = env->NewObject(pair_class, pair_constructor, result_str, probability_obj);

	free(result);
	env->ReleaseStringUTFChars(sentence, sentence_str);
	env->ReleaseStringUTFChars(root_nonterminal, nonterminal);
	return pair;
}
