package com.konrad.konradquiz.util;

import com.konrad.konradquiz.entity.Answer;
import com.konrad.konradquiz.entity.Answer.SdtCategory;
import com.konrad.konradquiz.entity.Question.CorrectAnswer;

public class SdtUtil {

    private SdtUtil() {}   // utility class — no instantiation

    /**
     * Classifies a participant's response using Signal Detection Theory.
     *
     * Score interpretation:
     *   0–49  = participant believes FAKE
     *   50    = unsure
     *   51–100 = participant believes REAL
     *
     * @param correctAnswer the ground truth (FAKE or REAL), nullable
     * @param score         participant's score 0–100
     * @return SdtCategory or null if correctAnswer not yet defined
     */
    public static SdtCategory classify(CorrectAnswer correctAnswer, int score, Answer.AnswerType answerType) {
        if (correctAnswer == null) return null;
        if (answerType == Answer.AnswerType.PROFILE) return null;

        // Both FAKE_DETECTION and MEMORY_TEST use -10 to 10 scale, midpoint is 0
        boolean participantSaidFake = score < 0;
        boolean participantSaidReal = score > 0;
        boolean participantUnsure   = score == 0;

        if (participantUnsure) return SdtCategory.UNSURE;

        return switch (correctAnswer) {
            case FAKE -> participantSaidFake ? SdtCategory.HIT : SdtCategory.MISS;
            case REAL -> participantSaidFake ? SdtCategory.FALSE_ALARM : SdtCategory.CORRECT_RESPONSE;
        };
    }
}