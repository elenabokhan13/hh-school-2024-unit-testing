package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {
  @Mock
  private NotificationService notificationService;
  @Mock
  private UserService userService;

  @InjectMocks
  private LibraryManager libraryManager;

  @Test
  void addBookFistTime() {
    libraryManager.addBook("Book1", 1);

    assertEquals(1, libraryManager.getAvailableCopies("Book1"));
  }

  @Test
  void addBookSecondTime() {
    libraryManager.addBook("Book2", 1);
    libraryManager.addBook("Book2", 2);

    assertEquals(3, libraryManager.getAvailableCopies("Book2"));
  }

  @Test
  void borrowBookAccountNotActive() {
    when(userService.isUserActive(anyString())).thenReturn(false);

    libraryManager.addBook("Book1", 1);

    assertFalse(libraryManager.borrowBook("Book1", "user1"));
    verify(notificationService, times(1)).notifyUser(anyString(), anyString());
  }

  @Test
  void borrowBookNoAvailableCopies() {
    when(userService.isUserActive(anyString())).thenReturn(true);

    assertFalse(libraryManager.borrowBook("Book1", "user1"));
    verify(notificationService, times(0)).notifyUser(anyString(), anyString());
  }

  @Test
  void borrowBookSuccess() {
    when(userService.isUserActive(anyString())).thenReturn(true);

    libraryManager.addBook("Book1", 1);
    boolean response = libraryManager.borrowBook("Book1", "user1");

    assertTrue(response);
    verify(notificationService, times(1)).notifyUser(anyString(), anyString());
    assertEquals(0, libraryManager.getAvailableCopies("Book1"));
  }

  @Test
  void returnBookNotBorrowed() {
    assertFalse(libraryManager.returnBook("Book1", "user1"));
  }

  @Test
  void returnBookBorrowedByOtherUser() {
    libraryManager.addBook("Book1", 1);
    libraryManager.borrowBook("Book1", "user1");
    assertFalse(libraryManager.returnBook("Book1", "user2"));
  }

  @Test
  void returnBookSuccess() {
    when(userService.isUserActive(anyString())).thenReturn(true);

    libraryManager.addBook("Book1", 1);
    libraryManager.borrowBook("Book1", "user1");
    boolean response = libraryManager.returnBook("Book1", "user1");

    assertTrue(response);
    assertEquals(1, libraryManager.getAvailableCopies("Book1"));
    verify(notificationService, times(2)).notifyUser(anyString(), anyString());
  }

  @Test
  void getAvailableCopiesNoBook() {
    assertEquals(0, libraryManager.getAvailableCopies("Book1"));
  }

  @Test
  void getAvailableCopiesSuccess() {
    libraryManager.addBook("Book1", 1);
    assertEquals(1, libraryManager.getAvailableCopies("Book1"));
  }

  @Test
  void calculateDynamicLateFeeNegativeOverdueDays() {
    var exception = assertThrows(IllegalArgumentException.class,
        () -> libraryManager.calculateDynamicLateFee(-2, false, false));

    assertEquals("Overdue days cannot be negative.", exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource({
      "1, true, false, 0.75",
      "1, false, true, 0.4",
      "1, true, true, 0.6",
      "1, false, false, 0.5",
      "0, false, false, 0"
  })
  void calculateFee(
      int overdueDays,
      boolean isBestseller,
      boolean isPremiumMember,
      double actualFee
  ) {
    double fee = libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember);

    assertEquals(actualFee, fee);
  }
}