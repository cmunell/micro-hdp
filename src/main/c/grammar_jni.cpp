/**
 * grammar_jni.cpp
 *
 *  Created on: May 20, 2015
 *      Author: asaparov
 */

#include "edu_cmu_ml_rtw_micro_hdp_HDPParser.h"

#include <stdlib.h>
#include <string.h>
#include "io.h"

extern "C" {
void run_console_file(FILE* input, const char* prompt);
void run_console_memory(memory_stream& input, const char* prompt);
void set_memory_limit(long unsigned int new_limit);
void set_data_path(const char* new_path);
char* parse_sentence(const char* sentence, const char* root_nonterminal, double log_k, double& probability);
}

wchar_t* get_string(JNIEnv* env, jstring str, size_t& char_count) {
	const char* text = env->GetStringUTFChars(str, 0);
	if (text == NULL) {
		fprintf(stderr, "(JNI) get_string ERROR: Unable to retrieve Java string.\n");
		return NULL;
	}

	char_count = mbstowcs(NULL, text, 0);
	wchar_t* wide_str = (wchar_t*) malloc(sizeof(wchar_t) * (char_count + 1));
	if (wide_str == NULL) {
		fprintf(stderr, "(JNI) get_string ERROR: Insufficient memory for wide character string.\n");
		env->ReleaseStringUTFChars(str, text);
		return NULL;
	}
	mbstowcs(wide_str, text, char_count);

	env->ReleaseStringUTFChars(str, text);
	return wide_str;
}

JNIEXPORT jboolean JNICALL Java_edu_cmu_ml_rtw_micro_hdp_HDPParser_initialize(
		JNIEnv* env, jobject obj, jstring init_script, jstring hdp_directory, jint memory_lim)
{
	set_memory_limit((memory_lim < 0) ? 0 : memory_lim);

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

	memory_stream stream(commands, (unsigned int) strlen(commands));
	run_console_memory(stream, "");
	env->ReleaseStringUTFChars(init_script, commands);

	return true;
}

JNIEXPORT jobject JNICALL Java_edu_cmu_ml_rtw_micro_hdp_HDPParser_annotate(
		JNIEnv* env, jobject obj, jstring sentence, jstring root_nonterminal, jdouble log_k)
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
	char* result = parse_sentence(sentence_str, nonterminal, log_k, probability);
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
