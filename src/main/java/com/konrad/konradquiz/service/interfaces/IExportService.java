package com.konrad.konradquiz.service.interfaces;

import jakarta.servlet.http.HttpServletResponse;

public interface IExportService {
    void exportToCsv(HttpServletResponse response);
}