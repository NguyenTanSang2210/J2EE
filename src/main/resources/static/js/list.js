// Book List API Functions

// Lấy CSRF token từ meta tag hoặc cookie
function getCsrfToken() {
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    return { token, header };
}

// Load danh sách sách từ API
function loadBooksFromAPI(pageNo = 0, pageSize = 20, sortBy = 'id') {
    $.ajax({
        url: `http://localhost:8080/api/v1/books?pageNo=${pageNo}&pageSize=${pageSize}&sortBy=${sortBy}`,
        type: 'GET',
        dataType: 'json',
        success: function(data) {
            renderBookTable(data);
        },
        error: function(xhr, status, error) {
            console.error('Error loading books:', error);
            alert('Failed to load books. Please try again.');
        }
    });
}

// Render bảng sách
function renderBookTable(books) {
    let tableHTML = '';
    
    $.each(books, function(index, book) {
        tableHTML += `<tr id="book-${book.id}">
            <td>${book.id}</td>
            <td>${book.title}</td>
            <td>${book.author}</td>
            <td>${book.price}</td>
            <td>${book.category}</td>
            <td>
                <a href="/books/edit/${book.id}" class="btn btn-primary btn-sm">Edit</a>
                <button class="btn btn-danger btn-sm" onclick="apiDeleteBook(${book.id}); return false;">Delete</button>
                <form action="/books/add-to-cart" method="post" class="d-inline">
                    <input type="hidden" name="id" value="${book.id}">
                    <input type="hidden" name="name" value="${book.title}">
                    <input type="hidden" name="price" value="${book.price}">
                    <button type="submit" class="btn btn-success btn-sm">Add to cart</button>
                </form>
            </td>
        </tr>`;
    });
    
    $('#book-table-body').html(tableHTML);
}

// Xóa sách bằng API
function apiDeleteBook(bookId) {
    if (!confirm('Are you sure you want to delete this book?')) {
        return false;
    }
    
    const { token, header } = getCsrfToken();
    
    $.ajax({
        url: `http://localhost:8080/api/v1/books/${bookId}`,
        type: 'DELETE',
        beforeSend: function(xhr) {
            if (token && header) {
                xhr.setRequestHeader(header, token);
            }
        },
        success: function() {
            alert('Book deleted successfully!');
            $(`#book-${bookId}`).fadeOut(300, function() {
                $(this).remove();
            });
        },
        error: function(xhr, status, error) {
            console.error('Error deleting book:', error);
            alert('Failed to delete book. Error: ' + (xhr.responseText || error));
        }
    });
    
    return false;
}

// Thêm sách bằng API
function apiAddBook(bookData) {
    const { token, header } = getCsrfToken();
    
    $.ajax({
        url: 'http://localhost:8080/api/v1/books',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(bookData),
        beforeSend: function(xhr) {
            if (token && header) {
                xhr.setRequestHeader(header, token);
            }
        },
        success: function(response) {
            alert('Book added successfully!');
            location.reload();
        },
        error: function(xhr, status, error) {
            console.error('Error adding book:', error);
            alert('Failed to add book. Error: ' + (xhr.responseText || error));
        }
    });
}

// Cập nhật sách bằng API
function apiUpdateBook(bookId, bookData) {
    const { token, header } = getCsrfToken();
    
    $.ajax({
        url: `http://localhost:8080/api/v1/books/${bookId}`,
        type: 'PUT',
        contentType: 'application/json',
        data: JSON.stringify(bookData),
        beforeSend: function(xhr) {
            if (token && header) {
                xhr.setRequestHeader(header, token);
            }
        },
        success: function(response) {
            alert('Book updated successfully!');
            location.reload();
        },
        error: function(xhr, status, error) {
            console.error('Error updating book:', error);
            alert('Failed to update book. Error: ' + (xhr.responseText || error));
        }
    });
}

// Khởi tạo khi document ready
$(document).ready(function() {
    console.log('Book List API functions loaded');
    
    // Có thể bật tính năng load từ API bằng cách uncomment dòng dưới:
    // loadBooksFromAPI();
});
