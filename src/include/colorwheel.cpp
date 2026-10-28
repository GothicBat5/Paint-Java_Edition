#ifndef UNICODE
#define UNICODE
#endif

#ifndef _UNICODE
#define _UNICODE
#endif

#include <windows.h>
#include <windowsx.h>
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cwchar>
#include <vector>

namespace {

constexpr wchar_t kClassName[] = L"ColorWheelWindow";
constexpr wchar_t kDefaultTitle[] = L"Color Wheel";
constexpr double kPi = 3.14159265358979323846;

struct Rgb {
    uint8_t r;
    uint8_t g;
    uint8_t b;
};

constexpr Rgb kBackground = {32, 32, 32};

// hue in degrees [0, 360), saturation and value in [0, 1].
Rgb HsvToRgb(double hue, double saturation, double value)
{
    const double chroma = value * saturation;
    const double sector = hue / 60.0;
    const double x = chroma * (1.0 - std::fabs(std::fmod(sector, 2.0) - 1.0));
    const double m = value - chroma;

    double r = 0.0;
    double g = 0.0;
    double b = 0.0;

    switch (static_cast<int>(sector)) 
    {
        case 0: r = chroma; g = x; b = 0.0; break;

        case 1: r = x; g = chroma; b = 0.0; break;

        case 2: r = 0.0; g = chroma; b = x; break;

        case 3: r = 0.0; g = x; b = chroma; break;

        case 4: r = x; g = 0.0; b = chroma; break;

        default: r = chroma; g = 0.0; b = x; break;
    }

    return Rgb {
        static_cast<uint8_t>(std::lround((r + m) * 255.0)),
        static_cast<uint8_t>(std::lround((g + m) * 255.0)),
        static_cast<uint8_t>(std::lround((b + m) * 255.0)),
    };
}

Rgb Blend(const Rgb& from, const Rgb& to, double amount)
{
    auto mix = [amount](uint8_t a, uint8_t b) {
        return static_cast<uint8_t>(std::lround(a + (b - a) * amount));
    };

    return Rgb{
        mix(from.r, to.r),
        mix(from.g, to.g),
        mix(from.b, to.b),
    };
}

uint32_t PackPixel(const Rgb& color)
{
    return (static_cast<uint32_t>(color.r) << 16) | (static_cast<uint32_t>(color.g) << 8) |
            static_cast<uint32_t>(color.b);
}

// ColorWheel: owns the pixel buffer and knows how to draw itself

class ColorWheel 
{
public:
    void Resize(int width, int height)
    {
        width_  = std::max(width, 1);
        height_ = std::max(height, 1);

        pixels_.assign(static_cast<size_t>(width_) * height_, PackPixel(kBackground));

        ZeroMemory(&bitmapInfo_, sizeof(bitmapInfo_));
        bitmapInfo_.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
        bitmapInfo_.bmiHeader.biWidth = width_;
        bitmapInfo_.bmiHeader.biHeight = -height_;  // negative = top-down rows
        bitmapInfo_.bmiHeader.biPlanes = 1;
        bitmapInfo_.bmiHeader.biBitCount = 32;
        bitmapInfo_.bmiHeader.biCompression = BI_RGB;

        Render();
    }

    void Draw(HDC dc) const
    {
        StretchDIBits(dc, 0, 0, width_, height_, 0, 0, width_, height_,
                      pixels_.data(),
                      &bitmapInfo_,
                      DIB_RGB_COLORS,
                      SRCCOPY);
    }

    // Returns true and fills `color` if the point lies on the wheel.
    bool ColorAt(int x, int y, Rgb& color) const
    {
        double coverage = 0.0;

        if (!Sample(x + 0.5, y + 0.5, color, coverage)) 
        {
            return false;
        }
        return coverage >= 1.0;
    }

private:
    static constexpr double kMargin = 16.0;

    double CenterX() 

    const { 
      return width_  / 2.0; 
    }

    double CenterY() 

    const { 
      return height_ / 2.0; 
    }

    double Radius() const
    {
        return std::min(width_, height_) / 2.0 - kMargin;
    }

    /*Computes the wheel color at a point. `coverage` is 1.0 well inside the
     wheel and fades to 0.0 across the outer edge (for anti-aliasing).
     Returns false when the point is entirely outside the wheel*/


    bool Sample(double x, double y, Rgb& color, double& coverage) const
    {
        const double radius = Radius();

        if (radius <= 0.0) 
        {
            return false;
        }

        const double dx = x - CenterX();
        const double dy = y - CenterY();
        const double distance = std::sqrt(dx * dx + dy * dy);

        coverage = std::clamp(radius - distance + 0.5, 0.0, 1.0);
        if (coverage <= 0.0) 
        {
            return false;
        }

        // Screen Y grows downward, so flip it to get a counter-clockwise angle.
        double hue = std::atan2(-dy, dx) * 180.0 / kPi;

        if (hue < 0.0) 
        {
            hue += 360.0;
        }

        const double saturation = std::min(distance / radius, 1.0);

        color = HsvToRgb(hue, saturation, 1.0);
        return true;
    }

    void Render()
    {
        for (int y = 0; y < height_; ++y) 
        {
            for (int x = 0; x < width_; ++x) 
            {
                Rgb color = kBackground;
                double coverage = 0.0;

                if (Sample(x + 0.5, y + 0.5, color, coverage)) 
                {
                    color = Blend(kBackground, color, coverage);
                }

                pixels_[static_cast<size_t>(y) * width_ + x] = PackPixel(color);
            }
        }
    }

    int width_ = 1;
    int height_ = 1;
    std::vector<uint32_t> pixels_;
    BITMAPINFO bitmapInfo_ = {};
};

ColorWheel g_wheel;

void OnPaint(HWND window)
{
    PAINTSTRUCT paint;
    HDC dc = BeginPaint(window, &paint);

    g_wheel.Draw(dc);

    EndPaint(window, &paint);
}

void OnSize(HWND window, LPARAM lParam)
{
    g_wheel.Resize(LOWORD(lParam), HIWORD(lParam));
    InvalidateRect(window, nullptr, FALSE);
}

void OnMouseMove(HWND window, LPARAM lParam)
{
    const int x = GET_X_LPARAM(lParam);
    const int y = GET_Y_LPARAM(lParam);

    Rgb color = {};

    if (g_wheel.ColorAt(x, y, color)) 
    {
        wchar_t title[96];

        _snwprintf(title, ARRAYSIZE(title), L"Color Wheel  -  #%02X%02X%02X  (R %u, G %u, B %u)",
                   color.r, color.g, color.b,
                   color.r, color.g, color.b);
        SetWindowTextW(window, title);
    } 

    else {
        SetWindowTextW(window, kDefaultTitle);
    }
}

LRESULT CALLBACK WindowProc(HWND window, UINT message, WPARAM wParam, LPARAM lParam)
{
    switch (message) 
    {
        case WM_PAINT:
            OnPaint(window);
            return 0;

        case WM_SIZE:
            OnSize(window, lParam);
            return 0;

        case WM_MOUSEMOVE:
            OnMouseMove(window, lParam);
            return 0;

        case WM_ERASEBKGND:
            // The wheel buffer covers the whole client area; skipping the
            // background erase is what prevents flicker while resizing.
            return 1;

        case WM_DESTROY:
            PostQuitMessage(0);
            return 0;
    }

    return DefWindowProcW(window, message, wParam, lParam);
}
  
  //setups

bool RegisterWindowClass(HINSTANCE instance)
{
    WNDCLASSEXW windowClass = {};
    windowClass.cbSize = sizeof(windowClass);
    windowClass.style = CS_HREDRAW | CS_VREDRAW;
    windowClass.lpfnWndProc = WindowProc;
    windowClass.hInstance = instance;
    windowClass.hCursor = LoadCursorW(nullptr, IDC_ARROW);
    windowClass.hbrBackground = nullptr;
    windowClass.lpszClassName = kClassName;

    return RegisterClassExW(&windowClass) != 0;
}

HWND CreateMainWindow(HINSTANCE instance)
{
    return CreateWindowExW(0, kClassName, kDefaultTitle,
                           WS_OVERLAPPEDWINDOW,
                           CW_USEDEFAULT, CW_USEDEFAULT,
                           640, 640, nullptr, nullptr, instance, nullptr);
}

int RunMessageLoop()
{
    MSG message;

    while (GetMessageW(&message, nullptr, 0, 0) > 0) 
    {
        TranslateMessage(&message);
        DispatchMessageW(&message);
    }
    return static_cast<int>(message.wParam);
}

}  // namespace

int WINAPI wWinMain(HINSTANCE instance, HINSTANCE, PWSTR, int showCommand)
{
    if (!RegisterWindowClass(instance)) 
    {
        return 1;
    }

    HWND window = CreateMainWindow(instance);
    if (window == nullptr) 
    {
        return 1;
    }

    ShowWindow(window, showCommand);
    UpdateWindow(window);

    return RunMessageLoop();
}
