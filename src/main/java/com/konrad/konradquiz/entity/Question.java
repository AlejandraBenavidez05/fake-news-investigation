package com.konrad.konradquiz.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @Column(name = "question_code", length = 10)
    private String questionCode;            // P1, P2 ... P17

    @Column(nullable = false, length = 100)
    private String constructo;              // Creador, Contenido, Víctimas, Contexto

    @Column(length = 100)
    private String subCategory;             // Humano Real, No Humano, Físico, etc. (nullable — merged cells)

    @Column(length = 100)
    private String subCategory2;            // Atributos de Fuente, titulares, etc.

    @Column(nullable = false, length = 500)
    private String itemText;                // the actual statement shown to participant

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private CorrectAnswer correctAnswer;    // nullable — you haven't defined all yet

    @Column(length = 200)
    private String referenceApa;

    @Column(length = 200)
    private String supportingQuote;

    public enum CorrectAnswer {
        FAKE, REAL
    }
}