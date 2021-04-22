/**
 *  Copyright 2021 Takayuki Maruyama
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bis5.excella.benchmark;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.bbreak.excella.reports.exporter.ExcelExporter;
import org.bbreak.excella.reports.model.ReportBook;
import org.bbreak.excella.reports.model.ReportSheet;
import org.bbreak.excella.reports.processor.ReportProcessor;
import org.bbreak.excella.reports.tag.RowRepeatParamParser;
import org.bbreak.excella.reports.tag.SingleParamParser;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

@State(Scope.Thread)
public class InvoiceReporterBenchmark {

    private ReportBook reportBook;
    private final URL xlsTemplate = InvoiceReporterBenchmark.class.getResource("template.xls");
    private final URL xlsxTemplate = InvoiceReporterBenchmark.class.getResource("template.xlsx");

    @Setup
    public void prepare() throws UnsupportedEncodingException {
        Path output = Paths.get(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        reportBook = new ReportBook((URL)null, output.toString(), ExcelExporter.FORMAT_TYPE);

        ReportSheet reportSheet = new ReportSheet("請求書");
        reportBook.addReportSheet(reportSheet);

        reportSheet.addParam(SingleParamParser.DEFAULT_TAG, "請求日付", new Date());
        reportSheet.addParam(SingleParamParser.DEFAULT_TAG, "顧客名称", "○○商事 様");

        URL imageFileUrl = InvoiceReporterBenchmark.class.getResource("logo.JPG");
        String imageFilePath = URLDecoder.decode(imageFileUrl.getPath(), "UTF-8");
        // reportSheet.addParam(ImageParamParser.DEFAULT_TAG, "会社ロゴ", imageFilePath);

        reportSheet.addParam(SingleParamParser.DEFAULT_TAG, "差出人住所1", "〒100-0000");
        reportSheet.addParam(SingleParamParser.DEFAULT_TAG, "差出人住所2", "東京都○○区○○○○ ×××ー×××");
        reportSheet.addParam(SingleParamParser.DEFAULT_TAG, "差出人住所3", "×××ビル 9F");

        List<String> productNameList = new ArrayList<>();
        productNameList.add("商品A");
        productNameList.add("商品B");
        productNameList.add("商品C");
        reportSheet.addParam(RowRepeatParamParser.DEFAULT_TAG, "商品名", productNameList.toArray());

        List<BigDecimal> unitPriceList = new ArrayList<>();
        unitPriceList.add(BigDecimal.valueOf(10000));
        unitPriceList.add(BigDecimal.valueOf(9000));
        unitPriceList.add(BigDecimal.valueOf(7000));
        reportSheet.addParam(RowRepeatParamParser.DEFAULT_TAG, "単価", unitPriceList.toArray());

        List<Integer> quantityList = new ArrayList<>();
        quantityList.add(4);
        quantityList.add(5);
        quantityList.add(6);
        reportSheet.addParam(RowRepeatParamParser.DEFAULT_TAG, "数量", quantityList.toArray());

        List<BigDecimal> priceList = new ArrayList<>();
        priceList.add(BigDecimal.valueOf(40000));
        priceList.add(BigDecimal.valueOf(45000));
        priceList.add(BigDecimal.valueOf(42000));
        reportSheet.addParam(RowRepeatParamParser.DEFAULT_TAG, "金額", priceList.toArray());

        BigDecimal subTotal = BigDecimal.ZERO;
        for (BigDecimal price : priceList) {
            subTotal = subTotal.add(price);
        }
        reportSheet.addParam(SingleParamParser.DEFAULT_TAG, "小計", subTotal);

        BigDecimal discount = BigDecimal.valueOf(10000);
        reportSheet.addParam(SingleParamParser.DEFAULT_TAG, "値引", discount);

        BigDecimal taxRate = BigDecimal.valueOf(0.05);
        BigDecimal taxCharge = subTotal.multiply(taxRate).setScale(0, RoundingMode.DOWN);
        reportSheet.addParam(SingleParamParser.DEFAULT_TAG, "税額", taxCharge);

        BigDecimal total = subTotal.add(taxCharge).subtract(discount);
        reportSheet.addParam(SingleParamParser.DEFAULT_TAG, "合計", total);
    }

    @TearDown
    public void cleanup() throws IOException {
        Files.deleteIfExists(Paths.get(reportBook.getOutputFileName() + ".xls"));
        Files.deleteIfExists(Paths.get(reportBook.getOutputFileName() + ".xlsx"));
    }

    @Benchmark
    public void testXls() throws Exception {
        reportBook.setTemplateFileURL(xlsTemplate);
        ReportProcessor processor = new ReportProcessor();
        processor.process(reportBook);
    }

    @Benchmark
    public void testXlsx() throws Exception {
        reportBook.setTemplateFileURL(xlsxTemplate);
        ReportProcessor processor = new ReportProcessor();
        processor.process(reportBook);
    }
}
