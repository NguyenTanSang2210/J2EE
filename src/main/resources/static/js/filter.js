// Filter.js - Tùy chọn: Tự động submit form khi thay đổi select
$(document).ready(function() {
    // Auto-submit on select change
    $('#filterForm select').on('change', function() {
        $('#filterForm').submit();
    });
    
    // Optional: AJAX filter (không reload trang)
    // Uncomment để sử dụng
    /*
    $('#filterForm').on('submit', function(e) {
        e.preventDefault();
        
        $.ajax({
            url: '/books/search',
            type: 'GET',
            data: $(this).serialize(),
            success: function(response) {
                // Update book list container
                $('.col-md-9').html($(response).find('.col-md-9').html());
            },
            error: function() {
                alert('Có lỗi xảy ra khi tìm kiếm!');
            }
        });
    });
    */
});
