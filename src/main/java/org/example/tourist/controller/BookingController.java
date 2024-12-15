package org.example.tourist.controller;
import org.example.tourist.models.User;
import org.example.tourist.models.Booking;
import org.example.tourist.BookingStatus;
import org.example.tourist.repositories.UserRepository;
import org.example.tourist.services.BookingService;
import org.example.tourist.BookingDto;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    public BookingController(BookingService bookingService, UserRepository userRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    /**
     * Создать новое бронирование.
     *
     * @param bookingDto DTO для создания бронирования
     * @param principal  информация о текущем пользователе
     * @param redirectAttributes атрибуты для перенаправления
     * @return перенаправление в зависимости от роли пользователя
     */
    /**
     * Создать новое бронирование.
     *
     * @param bookingDto          DTO для создания бронирования
     * @param principal           информация о текущем пользователе
     * @param redirectAttributes  атрибуты для перенаправления
     * @return перенаправление в зависимости от роли пользователя
     */
    @PostMapping("/create")
    public String createBooking(@ModelAttribute BookingDto bookingDto, Principal principal, RedirectAttributes redirectAttributes) {
        // Извлекаем имя пользователя из Principal
        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        bookingService.createBooking(bookingDto, user);

        Authentication authentication = (Authentication) principal;
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        if (userDetails.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
            redirectAttributes.addFlashAttribute("successMessage", "Бронирование успешно создано.");
            return "redirect:/bookings";
        } else if (userDetails.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER"))) {
            redirectAttributes.addFlashAttribute("successMessage", "Бронирование успешно создано.");
            return "redirect:/bookings/my-bookings";
        }

        return "redirect:/";
    }


    /**
     * Показать страницу с бронированиями.
     *
     * @param model модель для представления
     * @return название представления "bookings"
     */
    @GetMapping
    public String bookingsPage(Model model) {
        List<Booking> bookings = bookingService.getAllBookings();
        model.addAttribute("bookings", bookings);
        model.addAttribute("statuses", BookingStatus.values());
        return "bookings";
    }

    /**
     * Обновить статус бронирования из таблицы.
     *
     * @param bookingId ID бронирования
     * @param status    новый статус
     * @param redirectAttributes атрибуты для перенаправления
     * @return перенаправление на страницу бронирований
     */
    @PostMapping("/updateStatus")
    public String updateBookingStatusFromTable(@RequestParam Long bookingId, @RequestParam String status, RedirectAttributes redirectAttributes) {
        try {
            BookingStatus newStatus = BookingStatus.valueOf(status);
            bookingService.updateBookingStatus(bookingId, newStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Статус бронирования обновлен.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Некорректный статус бронирования.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/bookings";
    }

    /**
     * Подтвердить бронирование.
     *
     * @param id ID бронирования
     * @param redirectAttributes атрибуты для перенаправления
     * @return перенаправление на страницу бронирований
     */
    @PostMapping("/confirm/{id}")
    public String confirmBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookingService.confirmBooking(id);
            redirectAttributes.addFlashAttribute("successMessage", "Бронирование подтверждено.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/bookings";
    }

    /**
     * Отменить бронирование.
     *
     * @param id ID бронирования
     * @param redirectAttributes атрибуты для перенаправления
     * @return перенаправление на страницу бронирований
     */
    @PostMapping("/cancel/{id}")
    public String cancelBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookingService.cancelBooking(id);
            redirectAttributes.addFlashAttribute("successMessage", "Бронирование отменено.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/bookings";
    }

    /**
     * Обработчик для удаления бронирования
     *
     * @param bookingId ID бронирования для удаления
     * @param redirectAttributes атрибуты для перенаправления
     * @return перенаправление на страницу с бронированиями
     */
    @PostMapping("/delete")
    public String deleteBooking(@RequestParam Long bookingId, RedirectAttributes redirectAttributes) {
        boolean success = bookingService.deleteBooking(bookingId);

        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Бронирование успешно удалено.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Не удалось удалить бронирование.");
        }

        return "redirect:/bookings";
    }

    /**
     * Показать страницу с бронированиями текущего пользователя.
     *
     * @param model     модель для представления
     * @param principal информация о текущем пользователе
     * @return название представления "my-bookings"
     */
    @GetMapping("/my-bookings")
    public String myBookingsPage(Model model, Principal principal) {
        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        List<Booking> bookings = bookingService.getBookingsByUser(user);
        model.addAttribute("bookings", bookings);
        model.addAttribute("statuses", BookingStatus.values());
        return "bookings/my-bookings";  // Указание на папку bookings
    }



}
