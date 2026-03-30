package com.konrad.konradquiz.util;

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
    public static SdtCategory classify(CorrectAnswer correctAnswer, int score) {
        if (correctAnswer == null) return null;   // not classified yet

        boolean participantSaidFake = score < 50;
        boolean participantSaidReal = score > 50;
        boolean participantUnsure   = score == 50;

        if (participantUnsure) return SdtCategory.UNSURE;

        return switch (correctAnswer) {
            case FAKE -> participantSaidFake
                    ? SdtCategory.HIT           // (A) correct detection of fake
                    : SdtCategory.MISS;         // (C) missed the fake news

            case REAL -> participantSaidFake
                    ? SdtCategory.FALSE_ALARM   // (B) wrongly flagged real as fake
                    : SdtCategory.CORRECT_RESPONSE; // (D) correctly identified real
        };
    }
}