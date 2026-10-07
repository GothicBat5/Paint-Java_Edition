#include <windows.h>
#include <commdlg.h>
#include <fstream>
#include <sstream>
#include <string>

#define IDC_EDIT 1001
#define IDC_TOOLBAR 1002
#define IDC_STATUSBAR 1003

#define ID_FILE_NEW 2001
#define ID_FILE_OPEN 2002
#define ID_FILE_SAVE 2003
#define ID_FILE_EXIT 2004
#define ID_EDIT_UNDO 3001
#define ID_EDIT_CUT 3002
#define ID_EDIT_COPY 3003
#define ID_EDIT_PASTE 3004
#define ID_EDIT_SELALL 3005
#define ID_VIEW_FONT 4001
#define ID_HELP_ABOUT 5001

HWND hEdit, hStatus;
HFONT hFont;
bool isDirty = false;
char currentFile[MAX_PATH] = "";

void UpdateTitle(HWND hwnd)
{
    char title[MAX_PATH + 32];
    const char* name = (currentFile[0] != '\0') ? currentFile : "Untitled";
    wsprintfA(title, "%s%s - NoteWin", isDirty ? "*" : "", name);
    SetWindowTextA(hwnd, title);
}

void UpdateStatus()
{
    DWORD sel = SendMessage(hEdit, EM_GETSEL, 0, 0);
    int pos = LOWORD(sel);
    int line = (int)SendMessage(hEdit, EM_LINEFROMCHAR, pos, 0);
    int col = pos - (int)SendMessage(hEdit, EM_LINEINDEX, line, 0);

    int len = GetWindowTextLength(hEdit);
    std::string buf(len + 1, '\0');
    GetWindowTextA(hEdit, buf.data(), len + 1);
    int words = 0;
    bool inWord = false;
  
    for (char c : buf) 
    {
        if (isspace((unsigned char)c)) inWord = false;
          
        else if (!inWord) 
        { 
          inWord = true; 
          ++words; 
        }
    }

    char status[128];
    wsprintfA(status, "  Ln %d, Col %d     Words: %d     Chars: %d", line + 1, col + 1, words, len);
    SetWindowTextA(hStatus, status);
}

bool AskSaveIfDirty(HWND hwnd)
{
    if (!isDirty) return true;
    int r = MessageBoxA(hwnd, "Save changes?", "NoteWin",  MB_YESNOCANCEL | MB_ICONQUESTION);
    if (r == IDCANCEL) return false;
    if (r == IDNO) return true;
    SendMessage(hwnd, WM_COMMAND, ID_FILE_SAVE, 0);
    return !isDirty; 
}

void FileNew(HWND hwnd)
{
    if (!AskSaveIfDirty(hwnd)) return;
    SetWindowTextA(hEdit, "");
    currentFile[0] = '\0';
    isDirty = false;
    UpdateTitle(hwnd);
}

void FileOpen(HWND hwnd)
{
    if (!AskSaveIfDirty(hwnd)) return;
    OPENFILENAMEA ofn = {};
    char file[MAX_PATH] = "";
    ofn.lStructSize = sizeof(ofn);
    ofn.hwndOwner = hwnd;
    ofn.lpstrFilter = "Text Files\0*.txt\0All Files\0*.*\0";
    ofn.lpstrFile = file;
    ofn.nMaxFile = MAX_PATH;
    ofn.Flags = OFN_FILEMUSTEXIST | OFN_HIDEREADONLY;
    if (!GetOpenFileNameA(&ofn)) return;

    std::ifstream fs(file);
    if (!fs) 
    { 
        MessageBoxA(hwnd, "Cannot open file.", "Error", MB_ICONERROR); 
        return; 
    }
    
    std::ostringstream ss; ss << fs.rdbuf();
    std::string content = ss.str();
    SetWindowTextA(hEdit, content.c_str());
    lstrcpyA(currentFile, file);
    isDirty = false;
    UpdateTitle(hwnd);
    UpdateStatus();
}

void FileSave(HWND hwnd, bool saveAs = false)
{
    if (saveAs || currentFile[0] == '\0') 
    {
        OPENFILENAMEA ofn = {};
        char file[MAX_PATH] = "";
        
        if (currentFile[0]) lstrcpyA(file, currentFile);
        
        ofn.lStructSize = sizeof(ofn);
        ofn.hwndOwner = hwnd;
        ofn.lpstrFilter = "Text Files\0*.txt\0All Files\0*.*\0";
        ofn.lpstrFile = file;
        ofn.nMaxFile = MAX_PATH;
        ofn.lpstrDefExt = "txt";
        ofn.Flags = OFN_OVERWRITEPROMPT;
        
        if (!GetSaveFileNameA(&ofn)) return;
        lstrcpyA(currentFile, file);
    }
    int len = GetWindowTextLength(hEdit);
    std::string buf(len + 1, '\0');
    GetWindowTextA(hEdit, buf.data(), len + 1);
    std::ofstream fs(currentFile);
    
    if (!fs) 
    { 
        MessageBoxA(hwnd, "Cannot write file.", "Error", MB_ICONERROR); 
        return; 
    }
    
    fs << buf.c_str();
    isDirty = false;
    UpdateTitle(hwnd);
}

void ChooseEditorFont(HWND hwnd)
{
    LOGFONT lf = { /*LogOffFile*/ };
    if (hFont) GetObject(hFont, sizeof(lf), &lf);
    else {
        lf.lfHeight = -14;
        lstrcpyA(lf.lfFaceName, "Consolas");
    }
    
    CHOOSEFONT cf = {};
    cf.lStructSize = sizeof(cf);
    cf.hwndOwner = hwnd;
    cf.lpLogFont = &lf;
    cf.Flags = CF_SCREENFONTS | CF_INITTOLOGFONTSTRUCT;
    if (!ChooseFont(&cf)) return;
    HFONT newFont = CreateFontIndirect(&lf);
    SendMessage(hEdit, WM_SETFONT, (WPARAM)newFont, TRUE);
    
    if (hFont) DeleteObject(hFont);
    hFont = newFont;
}

HMENU BuildMenu()
{
    HMENU hMenu = CreateMenu();
    HMENU hFile = CreatePopupMenu();
    HMENU hEdit = CreatePopupMenu();
    HMENU hView = CreatePopupMenu();
    HMENU hHelp = CreatePopupMenu();

    AppendMenuA(hFile, MF_STRING, ID_FILE_NEW, "&New\tCtrl+N");
    AppendMenuA(hFile, MF_STRING, ID_FILE_OPEN, "&Open...\tCtrl+O");
    AppendMenuA(hFile, MF_STRING, ID_FILE_SAVE, "&Save\tCtrl+S");
    AppendMenuA(hFile, MF_SEPARATOR, 0, nullptr);
    AppendMenuA(hFile, MF_STRING, ID_FILE_EXIT, "E&xit");

    AppendMenuA(hEdit, MF_STRING, ID_EDIT_UNDO, "&Undo\tCtrl+Z");
    AppendMenuA(hEdit, MF_SEPARATOR, 0, nullptr);
    AppendMenuA(hEdit, MF_STRING, ID_EDIT_CUT, "Cu&t\tCtrl+X");
    AppendMenuA(hEdit, MF_STRING, ID_EDIT_COPY, "&Copy\tCtrl+C");
    AppendMenuA(hEdit, MF_STRING, ID_EDIT_PASTE, "&Paste\tCtrl+V");
    AppendMenuA(hEdit, MF_SEPARATOR, 0, nullptr);
    AppendMenuA(hEdit, MF_STRING, ID_EDIT_SELALL, "Select &All\tCtrl+A");

    AppendMenuA(hView, MF_STRING, ID_VIEW_FONT, "&Font...");
    AppendMenuA(hHelp, MF_STRING, ID_HELP_ABOUT, "&About");

    AppendMenuA(hMenu, MF_POPUP, (UINT_PTR)hFile, "&File");
    AppendMenuA(hMenu, MF_POPUP, (UINT_PTR)hEdit, "&Edit");
    AppendMenuA(hMenu, MF_POPUP, (UINT_PTR)hView, "&View");
    AppendMenuA(hMenu, MF_POPUP, (UINT_PTR)hHelp, "&Help");
  
    return hMenu;
}


LRESULT CALLBACK WindowProc(HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam)
{
    switch (uMsg)
    {
    case WM_CREATE:
    {
        hEdit = CreateWindowExA(WS_EX_CLIENTEDGE, "EDIT", "",
            WS_CHILD | WS_VISIBLE | WS_VSCROLL | WS_HSCROLL |
            ES_MULTILINE | ES_AUTOVSCROLL | ES_AUTOHSCROLL | ES_NOHIDESEL,
            0, 0, 0, 0, hwnd, (HMENU)IDC_EDIT,
            ((LPCREATESTRUCT)lParam)->hInstance, nullptr);


        hStatus = CreateWindowExA(0, "STATIC", "  Ln 1, Col 1",
            WS_CHILD | WS_VISIBLE | SS_LEFT,
            0, 0, 0, 0, hwnd, (HMENU)IDC_STATUSBAR,
            ((LPCREATESTRUCT)lParam)->hInstance, nullptr);

        // Nice monospace font
        hFont = CreateFontA(-16, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE,
                 DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY,
                 FIXED_PITCH, "Consolas");
        SendMessage(hEdit, WM_SETFONT, (WPARAM)hFont, TRUE);

        HFONT hUiFont = (HFONT)GetStockObject(DEFAULT_GUI_FONT);
        SendMessage(hStatus, WM_SETFONT, (WPARAM)hUiFont, TRUE);

        SendMessage(hEdit, EM_SETLIMITTEXT, 64 * 1024 * 1024, 0);

        UpdateTitle(hwnd);
        break;
    }

    case WM_SIZE:
    {
        int W = LOWORD(lParam), H = HIWORD(lParam);
        int statusH = 24;
        SetWindowPos(hEdit, nullptr, 0, 0,  W, H - statusH, SWP_NOZORDER);
        SetWindowPos(hStatus, nullptr, 0, H - statusH, W, statusH,  SWP_NOZORDER);
        break;
    }

    case WM_COMMAND:
        switch (LOWORD(wParam))
        {

        case IDC_EDIT:
            if (HIWORD(wParam) == EN_CHANGE) {
                isDirty = true;
                UpdateTitle(hwnd);
                UpdateStatus();
            }
            if (HIWORD(wParam) == EN_UPDATE) UpdateStatus();
            break;
        case ID_FILE_NEW: FileNew(hwnd); break;
        case ID_FILE_OPEN: FileOpen(hwnd); break;
        case ID_FILE_SAVE: FileSave(hwnd); break;
        case ID_FILE_EXIT: SendMessage(hwnd, WM_CLOSE, 0, 0); break;
        case ID_EDIT_UNDO: SendMessage(hEdit, WM_UNDO, 0, 0); break;
        case ID_EDIT_CUT: SendMessage(hEdit, WM_CUT, 0, 0); break;
        case ID_EDIT_COPY: SendMessage(hEdit, WM_COPY, 0, 0); break;
        case ID_EDIT_PASTE: SendMessage(hEdit, WM_PASTE, 0, 0); break;
        case ID_EDIT_SELALL:SendMessage(hEdit, EM_SETSEL, 0, -1); break;
        case ID_VIEW_FONT: ChooseEditorFont(hwnd); break;

        case ID_HELP_ABOUT:
            MessageBoxA(hwnd,
                "NoteWin — Win32 Text Editor Demo\n\nBuilt with pure Win32 API.",
                "About NoteWin", MB_ICONINFORMATION);
            break;
        }
        break;

    case WM_KEYDOWN:
        if (GetKeyState(VK_CONTROL) & 0x8000) 
        {
            switch (wParam) 
            {
            case 'N': SendMessage(hwnd, WM_COMMAND, ID_FILE_NEW,  0); break;
            case 'O': SendMessage(hwnd, WM_COMMAND, ID_FILE_OPEN, 0); break;
            case 'S': SendMessage(hwnd, WM_COMMAND, ID_FILE_SAVE, 0); break;
            }
        }
        break;

    case WM_CLOSE:
        if (AskSaveIfDirty(hwnd))
            DestroyWindow(hwnd);
        break;

    case WM_DESTROY:
        if (hFont) DeleteObject(hFont);
        PostQuitMessage(0);
        break;
    }
    return DefWindowProc(hwnd, uMsg, wParam, lParam);
}


int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE, LPSTR lpCmdLine, int nCmdShow)
{
    const char CLASS_NAME[] = "NoteWinClass";
    WNDCLASSA wc = {};
    wc.lpfnWndProc = WindowProc;
    wc.hInstance = hInstance;
    wc.lpszClassName = CLASS_NAME;
    wc.hCursor = LoadCursor(nullptr, IDC_ARROW);
    wc.hbrBackground = (HBRUSH)(COLOR_WINDOW + 1);
    wc.lpszMenuName = nullptr;
    RegisterClassA(&wc);

    HWND hwnd = CreateWindowExA(0, CLASS_NAME, "NoteWin",
        WS_OVERLAPPEDWINDOW, CW_USEDEFAULT, CW_USEDEFAULT, 900, 650,
        nullptr, BuildMenu(), hInstance, nullptr);

    if (lpCmdLine && lpCmdLine[0] != '\0') 
    {
        lstrcpyA(currentFile, lpCmdLine);

        if (currentFile[0] == '"') 
        {
            lstrcpyA(currentFile, currentFile + 1);
            currentFile[lstrlenA(currentFile) - 1] = '\0';
        }
        std::ifstream fs(currentFile);
        
        if (fs) 
        {
            std::ostringstream ss; ss << fs.rdbuf();
            SetWindowTextA(hEdit, ss.str().c_str());
            UpdateTitle(hwnd);
        }
    }

    ShowWindow(hwnd, nCmdShow);
    UpdateWindow(hwnd);

    MSG msg = {};
    while (GetMessage(&msg, nullptr, 0, 0)) 
    {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
    }
    return (int)msg.wParam;
}
