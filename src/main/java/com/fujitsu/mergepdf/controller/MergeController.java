package com.fujitsu.mergepdf.controller;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.fujitsu.mergepdf.FileUploadUtil;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

@Controller
public class MergeController {
	
	@GetMapping("/mergepdf")
	public String mergePdfForm() {
		return "mergepdf";
	}
	
	@PostMapping("/mergepdf")
	public String mergePdfProcess(@RequestParam("upload") List<MultipartFile> files, Model m) {
		//System.out.println(files.get(0).getOriginalFilename()+" "+files.get(1).getOriginalFilename());
        String uploadDir = "D:\\java\\Spring\\boot\\SpringMergePdf\\src\\main\\resources\\static\\";
        try {
        	//System.out.println("FileUploadUtil ");
        	for(int i=0;i<files.size();i++) {
        		String fileName = StringUtils.cleanPath(files.get(i).getOriginalFilename());
        		FileUploadUtil.saveFile(uploadDir+"uploads\\", fileName, files.get(i));
			}
        } catch(IOException ie) {
        	System.out.println("Error! file upload "+ie.getMessage());
        	m.addAttribute("error_message", "Please select a pdf file");
        }
		try {
			List<InputStream> inputPdfList = new ArrayList<InputStream>();
			for(int i=0;i<files.size();i++) {
				inputPdfList.add(new FileInputStream(uploadDir+"uploads\\"+StringUtils.cleanPath(files.get(i).getOriginalFilename())));
			}
			OutputStream outputStream = new FileOutputStream(uploadDir+"mergedpdfs\\"+"merged"+StringUtils.cleanPath(files.get(0).getOriginalFilename()));
			Document document = new Document();
			List<PdfReader> readers = new ArrayList<PdfReader>();
			int totalPages=0;
			Iterator<InputStream> pdfIterator = inputPdfList.iterator();
			while(pdfIterator.hasNext()) {
				InputStream pdf = pdfIterator.next();
				PdfReader pdfreader = new PdfReader(pdf);
				readers.add(pdfreader);
				totalPages = totalPages+pdfreader.getNumberOfPages();
			}
			PdfWriter writer = PdfWriter.getInstance(document, outputStream);
			document.open();
			PdfContentByte pageContentByte = writer.getDirectContent();
			PdfImportedPage pdfImportedPage;
			int currentPdfReaderPage=1;
			Iterator<PdfReader> iteratorPdfReader = readers.iterator();
			while(iteratorPdfReader.hasNext()) {
				PdfReader pdfReader = iteratorPdfReader.next();
				while(currentPdfReaderPage<=pdfReader.getNumberOfPages()) {
					document.newPage();
					pdfImportedPage = writer.getImportedPage(pdfReader, currentPdfReaderPage);
					pageContentByte.addTemplate(pdfImportedPage, 0, 0);
					currentPdfReaderPage++;
				}
				currentPdfReaderPage=1;
			}
			String url = "merged"+StringUtils.cleanPath(files.get(0).getOriginalFilename());
    		m.addAttribute("downloadurl", url);
    		m.addAttribute("success_message", "Successfully converted "+files.get(0).getOriginalFilename()+" to "+url+". You can now download the converted file below.");
			outputStream.flush();
			document.close();
			outputStream.close();
		}
		catch(Exception e) {
			e.printStackTrace();
			m.addAttribute("error_message", "File Conversion failed! please try again.");
		}
		return "mergepdf";
	}
	
	@GetMapping("/download/{fileName:.+}")
	public ResponseEntity downloadFileFromLocal(@PathVariable String fileName) {
		String uploadDir =  "D:\\java\\Spring\\boot\\SpringMergePdf\\src\\main\\resources\\static\\mergedpdfs\\";;
		Path path = Paths.get(uploadDir + fileName);
		Resource resource = null;
		try {
			resource = new UrlResource(path.toUri());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("application/x-pdf"))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
				.body(resource);
	}

}
