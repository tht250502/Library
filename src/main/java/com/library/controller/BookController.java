package com.library.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.library.model.Book;
import com.library.model.Category;
import com.library.model.Publisher;
import com.library.model.User;
import com.library.service.BookService;
import com.library.service.CategoryService;
import com.library.service.PublisherService;
import com.library.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;


@Controller
@RequestMapping("/admin")
public class BookController {

    @Autowired
    private PublisherService publisherService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    @ModelAttribute
    public void getUserDetails(Principal p, Model m) {
        if (p != null) {
            String email = p.getName();
            User userDtls = userService.getUserByEmail(email);
            if (userDtls != null) {
                m.addAttribute("user", userDtls);
            }
        }

        List<Category> allActiveCategory = categoryService.getAllActiveCategory();
        m.addAttribute("categorys", allActiveCategory);
    }

    @GetMapping("/loadAddBook")
    public String loadAddProduct(Model m) {
        List<Category> categories = categoryService.getAllCategory();
        m.addAttribute("categories", categories);
        List<Publisher> publishers = publisherService.getAllPublisher();
        m.addAttribute("publishers", publishers);
        return "admin/add_book";
    }

    @PostMapping("/saveBook")
    public String saveProduct(@ModelAttribute Book book, @RequestParam("file") MultipartFile image,
                              HttpSession session) throws IOException {

        String imageName = image.isEmpty() ? "default.jpg" : image.getOriginalFilename();

        book.setImage(imageName);
        book.setDiscount(0);
        book.setDiscountPrice(book.getPrice());
        book.setCreatedDate(LocalDateTime.now());
        Book saveBook = bookService.saveBook(book);

        if (!ObjectUtils.isEmpty(saveBook)) {

            File saveFile = new ClassPathResource("static/images").getFile();

            Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "book_img" + File.separator
                    + image.getOriginalFilename());

            // System.out.println(path);
            Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            session.setAttribute("succMsg", "Sách được thêm thành công");
        } else {
            session.setAttribute("errorMsg", "Không thêm được sách");
        }

        return "redirect:/admin/loadAddBook";
    }

    @GetMapping("/view_admin_book")
    public String loadViewBook(Model m, @RequestParam(defaultValue = "") String ch,
                               @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
                               @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
//		List<Book> books = null;
//		if (ch != null && ch.length() > 0) {
//			books = bookService.searchBook(ch);
//		} else {
//			books = bookService.getAllBooks();
//		}
//		m.addAttribute("books", books);
        Page<Book> page = null;
        if (ch != null && ch.length() > 0) {
            page = bookService.searchBookPagination(pageNo, pageSize, ch);
        } else {
            page = bookService.getAllBooksPagination(pageNo, pageSize);
        }
        m.addAttribute("books", page.getContent());

        m.addAttribute("pageNo", page.getNumber());
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());
        return "admin/view_admin_book";
    }

    @GetMapping("/deleteBook/{id}")
    public String deleteBook(@PathVariable int id, HttpSession session) {
        Boolean deleteBook = bookService.deleteBook(id);
        if (deleteBook) {
            session.setAttribute("succMsg", "Sách được xóa khỏi hệ thống");
        } else {
            session.setAttribute("errorMsg", "Có lỗi xảy ra");
        }
        return "redirect:/admin/view_admin_book";
    }

    @GetMapping("/editBook/{id}")
    public String editBook(@PathVariable int id, Model m) {
        m.addAttribute("book", bookService.getBookById(id));
        m.addAttribute("categories", categoryService.getAllCategory());
        m.addAttribute("publishers", publisherService.getAllPublisher());
        return "admin/edit_book";
    }

    @PostMapping("/updateBook")
    public String updateBook(@ModelAttribute Book book, @RequestParam("file") MultipartFile image,
                             HttpSession session, Model m) {


        Book updateBook = bookService.updateBook(book, image);
        if (!ObjectUtils.isEmpty(updateBook)) {
            session.setAttribute("succMsg", "Sách được cập nhật thành công");
        } else {
            session.setAttribute("errorMsg", "Có lỗi xảy ra");
        }

        return "redirect:/admin/editBook/" + book.getId();
    }

    // Add book từ file excel
    @PostMapping("/uploadBooks")
    public String uploadBooks(@RequestParam("file") MultipartFile file, HttpSession session) {
        try {
            if (file.isEmpty()) {
                session.setAttribute("errorMsg", "Vui lòng chọn file Excel!");
                return "redirect:/admin/add_book";
            }

            bookService.saveBooksFromExcel(file);
            session.setAttribute("succMsg", "Sách được thêm thành công từ file Excel!");
        } catch (Exception e) {
            session.setAttribute("errorMsg", "Lỗi khi thêm sách: " + e.getMessage());
        }
        return "redirect:/admin/view_admin_book";
    }
}
