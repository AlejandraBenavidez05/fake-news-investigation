package com.konrad.konradquiz.util;

import com.konrad.konradquiz.entity.Answer;
import com.konrad.konradquiz.entity.Answer.SdtCategory;
import com.konrad.konradquiz.entity.Question.CorrectAnswer;

public final class SdtUtil {

    private SdtUtil() {
        // utility class — no instantiation
    }

    /**
     * Classifies a participant's response using Signal Detection Theory.
     *
     * <p>News answers (FAKE_DETECTION and MEMORY_TEST) use a -10..10 scale:
     * negative = participant believes FAKE, 0 = unsure, positive = participant believes REAL.
     *
     * @param correctAnswer the ground truth (FAKE or REAL), nullable
     * @param score         participant's score (-10..10)
     * @param answerType    PROFILE answers are never classified
     * @return SdtCategory, or null for PROFILE answers and questions without a defined correct answer
     */
    public static SdtCategory classify(CorrectAnswer correctAnswer, int score, Answer.AnswerType answerType) {
        if (correctAnswer == null || answerType == Answer.AnswerType.PROFILE) {
            return null;
        }
        if (score == 0) {
            return SdtCategory.UNSURE;
        }

        boolean participantSaidFake = score < 0;
        return switch (correctAnswer) {
            case FAKE -> participantSaidFake ? SdtCategory.HIT : SdtCategory.MISS;
            case REAL -> participantSaidFake ? SdtCategory.FALSE_ALARM : SdtCategory.CORRECT_RESPONSE;
        };
    }
}
