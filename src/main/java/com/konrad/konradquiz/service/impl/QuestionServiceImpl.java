package com.konrad.konradquiz.service.impl;

import com.konrad.konradquiz.dto.request.QuestionCorrectAnswerRequestDto;
import com.konrad.konradquiz.dto.response.QuestionAdminResponseDto;
import com.konrad.konradquiz.entity.Question;
import com.konrad.konradquiz.exception.ResourceNotFoundException;
import com.konrad.konradquiz.mapper.QuestionMapper;
import com.konrad.konradquiz.repository.QuestionRepository;
import com.konrad.konradquiz.service.interfaces.IQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestionServiceImpl implements IQuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionMapper questionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<Question> getAllForSession() {
        return questionRepository.findAll(); // Question logic lives here, one place only
    }
    @Override
    public QuestionAdminResponseDto updateCorrectAnswer(String questionCode, QuestionCorrectAnswerRequestDto dto) {
        Question question = questionRepository.findById(questionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionCode));

        question.setCorrectAnswer(dto.getCorrectAnswer());
        Question saved = questionRepository.save(question);

        return QuestionAdminResponseDto.builder()
                .questionCode(saved.getQuestionCode())
                .itemText(saved.getItemText())
                .correctAnswer(saved.getCorrectAnswer())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Question findEntityByCode(String questionCode) {
        return questionRepository.findById(questionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionCode));
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return questionRepository.count();
    }
}