package com.library.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.library.model.User;
import com.library.service.CartService;
import com.library.service.CategoryService;
import com.library.service.OrderService;
import com.library.service.UserService;
import com.library.util.CommonUtil;
import com.library.util.OrderStatus;
import com.library.model.BookOrder;
import com.library.model.BookOrderItem;
import com.library.model.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpSession;


@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private CartService cartService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CommonUtil commonUtil;


    @Autowired
    private PasswordEncoder passwordEncoder;

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

    @GetMapping("/")
    public String index() {
        return "admin/index";
    }

    @GetMapping("/users")
    public String getAllUsers(Model m, @RequestParam Integer type) {
        List<User> users = null;
        if (type == 1) {
            users = userService.getUsers("ROLE_USER");
        } else {
            users = userService.getUsers("ROLE_ADMIN");
        }
        m.addAttribute("userType",type);
        m.addAttribute("users", users);
        return "/admin/users";
    }

    @GetMapping("/updateSts")
    public String updateUserAccountStatus(@RequestParam Boolean status, @RequestParam Integer id, HttpSession session) {
        Boolean f = userService.updateAccountStatus(id, status);
        if (f) {
            session.setAttribute("succMsg", "Đã cập nhật tài khoản");
        } else {
            session.setAttribute("errorMsg", "Đã xảy ra lỗi");
        }
        return "redirect:/admin/users?type=\"+type";
    }

    @GetMapping("/category")
    public String category(Model m, @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
                           @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        // m.addAttribute("categorys", categoryService.getAllCategory());
        Page<Category> page = categoryService.getAllCategoryPagination(pageNo, pageSize);
        List<Category> categorys = page.getContent();
        m.addAttribute("categorys", categorys);

        m.addAttribute("pageNo", page.getNumber());
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());

        return "admin/category";
    }

    @PostMapping("/saveCategory")
    public String saveCategory(@ModelAttribute Category category, @RequestParam("file") MultipartFile file,
                               HttpSession session) throws IOException {

        String imageName = file != null ? file.getOriginalFilename() : "default.jpg";
        category.setImageName(imageName);

        Boolean existCategory = categoryService.existCategory(category.getImageName());

        if (existCategory) {
            session.setAttribute("errorMsg", "Tên danh mục đã tồn tại");
        } else {

            Category saveCategory = categoryService.saveCategory(category);

            if (ObjectUtils.isEmpty(saveCategory)) {
                session.setAttribute("errorMsg", "Lưu không thành công! lỗi máy chủ");
            } else {

                File saveFile = new ClassPathResource("static/images").getFile();

                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "category_img" + File.separator
                        + file.getOriginalFilename());

                // System.out.println(path);
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

                session.setAttribute("succMsg", "Lưu thành công");
            }
        }

        return "redirect:/admin/category";
    }

    @GetMapping("/deleteCategory/{id}")
    public String deleteCategory(@PathVariable int id, HttpSession session) {
        Boolean deleteCategory = categoryService.deleteCategory(id);

        if (deleteCategory) {
            session.setAttribute("succMsg", "Xóa danh mục thành công");
        } else {
            session.setAttribute("errorMsg", "Lỗi máy chủ");
        }

        return "redirect:/admin/category";
    }

    @GetMapping("/loadEditCategory/{id}")
    public String loadEditCategory(@PathVariable int id, Model m) {
        m.addAttribute("category", categoryService.getCategoryById(id));
        return "admin/edit_category";
    }

    @PostMapping("/updateCategory")
    public String updateCategory(@ModelAttribute Category category, @RequestParam("file") MultipartFile file,
                                 HttpSession session) throws IOException {

        Category oldCategory = categoryService.getCategoryById(category.getId());
        String imageName = file.isEmpty() ? oldCategory.getImageName() : file.getOriginalFilename();

        if (!ObjectUtils.isEmpty(category)) {

            oldCategory.setName(category.getName());
            oldCategory.setIsActive(category.getIsActive());
            oldCategory.setImageName(imageName);
        }

        Category updateCategory = categoryService.saveCategory(oldCategory);

        if (!ObjectUtils.isEmpty(updateCategory)) {

            if (!file.isEmpty()) {
                File saveFile = new ClassPathResource("static/images").getFile();

                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "category_img" + File.separator
                        + file.getOriginalFilename());

                // System.out.println(path);
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            }

            session.setAttribute("succMsg", "Danh mục sách cập nhật thành công");
        } else {
            session.setAttribute("errorMsg", "Danh mục sách cập nhật không thành công");
        }

        return "redirect:/admin/loadEditCategory/" + category.getId();
    }

    @GetMapping("/orders")
    public String getAllOrders(Model m,@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
                               @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
//		List<BookOrder> allOrders = orderService.getAllOrders();
        Page<BookOrder> page = orderService.getAllOrdersPagination(pageNo, pageSize);


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        // Tạo DecimalFormat để định dạng totalAmount
        DecimalFormat decimalFormat = new DecimalFormat("###,###,###");

        for (BookOrder order : page) {
            // Đổi ngày giờ theo định dạng dd/MM/yyyy HH:mm:ss
            if (order.getOrderDate() != null) {
                String formattedDateTime = order.getOrderDate().format(formatter);
                order.setFormattedOrderDate(formattedDateTime); // Cập nhật ngày giờ đã định dạng
            }

            // Tính tổng giá trị của đơn hàng và định dạng số tiền
            int totalAmount = 20000;
            for (BookOrderItem item : order.getItems()) {
                totalAmount += item.getBook().getDiscountPrice() * item.getQuantity();
            }
            order.setTotalAmount(totalAmount); // Lưu tổng giá trị vào đơn hàng

            // Định dạng totalAmount với dấu phân cách hàng nghìn
            String formattedTotalAmount = decimalFormat.format(totalAmount);
            order.setFormattedTotalAmount(formattedTotalAmount); // Lưu tổng giá trị đã định dạng
        }
//		m.addAttribute("orders", allOrders);
        m.addAttribute("orders", page.getContent());
        m.addAttribute("srch", false);

        m.addAttribute("pageNo", page.getNumber());
        m.addAttribute("pageSize", pageSize);
        m.addAttribute("totalElements", page.getTotalElements());
        m.addAttribute("totalPages", page.getTotalPages());
        m.addAttribute("isFirst", page.isFirst());
        m.addAttribute("isLast", page.isLast());

        return "/admin/orders";
    }

    @PostMapping("/update-order-status")
    public String updateOrderStatus(@RequestParam Integer id, @RequestParam Integer st, HttpSession session) {

        OrderStatus[] values = OrderStatus.values();
        String status = null;

        for (OrderStatus orderSt : values) {
            if (orderSt.getId().equals(st)) {
                status = orderSt.getName();
            }
        }

        BookOrder updateOrder = orderService.updateOrderStatus(id, status);

        try {
            commonUtil.sendMailForBookOrder(updateOrder, status);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!ObjectUtils.isEmpty(updateOrder)) {
            session.setAttribute("succMsg", "Đã cập nhật trạng thái");
        } else {
            session.setAttribute("errorMsg", "Trạng thái chưa được cập nhật");
        }
        return "redirect:/admin/orders";
    }

    @GetMapping("/monthly")
    public ResponseEntity<Map<Integer, Integer>> getMonthlyRevenue(
            @RequestParam(name = "year", defaultValue = "2024") int year) {
        Map<Integer, Integer> monthlyRevenue = orderService.getMonthlyRevenue(year);
        return ResponseEntity.ok(monthlyRevenue);
    }

    @GetMapping("/daily-revenue")
    public ResponseEntity<Map<Integer, Integer>> getDailyRevenue(@RequestParam int year, @RequestParam int month) {


        Map<Integer, Integer> dailyRevenue = orderService.getDailyRevenue(year, month);


        return ResponseEntity.ok(dailyRevenue);
    }


    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getOrderStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(orderService.getOrderStatistics(startDate, endDate));
    }

    @GetMapping("/search-order")
    public String searchProduct(@RequestParam String orderId, Model m, HttpSession session,@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
                                @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {

        if (orderId != null && orderId.length() > 0) {

            BookOrder order = orderService.getOrdersByOrderId(orderId.trim());

            if (ObjectUtils.isEmpty(order)) {
                session.setAttribute("errorMsg", "Không thấy mã vận đơn");
                m.addAttribute("orderDtls", null);
            } else {
                m.addAttribute("orderDtls", order);
            }

            m.addAttribute("srch", true);
        } else {
//			List<BookOrder> allOrders = orderService.getAllOrders();
//			m.addAttribute("orders", allOrders);
//			m.addAttribute("srch", false);


            Page<BookOrder> page = orderService.getAllOrdersPagination(pageNo, pageSize);
            m.addAttribute("orders", page);
            m.addAttribute("srch", false);

            m.addAttribute("pageNo", page.getNumber());
            m.addAttribute("pageSize", pageSize);
            m.addAttribute("totalElements", page.getTotalElements());
            m.addAttribute("totalPages", page.getTotalPages());
            m.addAttribute("isFirst", page.isFirst());
            m.addAttribute("isLast", page.isLast());
        }
        return "/admin/orders";

    }

    @GetMapping("/add-admin")
    public String loadAdminAdd() {
        return "/admin/add_admin";
    }

    @PostMapping("/save-admin")
    public String saveAdmin(@ModelAttribute User user, @RequestParam("img") MultipartFile file, HttpSession session)
            throws IOException {

        String imageName = file.isEmpty() ? "default.jpg" : file.getOriginalFilename();
        user.setProfileImage(imageName);
        User saveUser = userService.saveAdmin(user);

        if (!ObjectUtils.isEmpty(saveUser)) {
            if (!file.isEmpty()) {
                File saveFile = new ClassPathResource("static/images").getFile();

                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "profile_img" + File.separator
                        + file.getOriginalFilename());

//				System.out.println(path);
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            }
            session.setAttribute("succMsg", "Tạo thành công");
        } else {
            session.setAttribute("errorMsg", "Lỗi máy chủ");
        }

        return "redirect:/admin/add-admin";
    }

    @GetMapping("/profile")
    public String profile() {
        return "/admin/profile";
    }

    @PostMapping("/update-profile")
    public String updateProfile(@ModelAttribute User user, @RequestParam MultipartFile img, HttpSession session) {
        User updateUserProfile = userService.updateUserProfile(user, img);
        if (ObjectUtils.isEmpty(updateUserProfile)) {
            session.setAttribute("errorMsg", "Profile not updated");
        } else {
            session.setAttribute("succMsg", "Profile Updated");
        }
        return "redirect:/admin/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String newPassword, @RequestParam String currentPassword, Principal p,
                                 HttpSession session) {
        User loggedInUserDetails = commonUtil.getLoggedInUserDetails(p);

        boolean matches = passwordEncoder.matches(currentPassword, loggedInUserDetails.getPassword());

        if (matches) {
            String encodePassword = passwordEncoder.encode(newPassword);
            loggedInUserDetails.setPassword(encodePassword);
            User updateUser = userService.updateUser(loggedInUserDetails);
            if (ObjectUtils.isEmpty(updateUser)) {
                session.setAttribute("errorMsg", "Password not updated !! Error in server");
            } else {
                session.setAttribute("succMsg", "Password Updated sucessfully");
            }
        } else {
            session.setAttribute("errorMsg", "Current Password incorrect");
        }

        return "redirect:/admin/profile";
    }
}
