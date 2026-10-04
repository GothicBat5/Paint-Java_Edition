#include <filesystem>
#include <iostream>
#include <vector>
namespace fs = std::filesystem;

class FileManager {
public:
    explicit FileManager(fs::path current) : cwd_(std::move(current)) {}

    void ls() 
    {
        std::cout << "CWD: " << cwd_ << "\n";
      
        for (auto& entry : fs::directory_iterator(cwd_)) 
        {
            auto status = entry.status();
            char type = fs::is_directory(status) ? 'd' : 'f';
            std::cout << type << "  " << entry.path().filename().string();
          
            if (fs::is_regular_file(status)) 
            {
                std::cout << "  (" << fs::file_size(entry) << " bytes)";
            }
            std::cout << "\n";
        }
    }

    void cd(const std::string& dir) 
    {
        fs::path new_path = cwd_ / dir;
      
        if (fs::exists(new_path) && fs::is_directory(new_path)) 
        {
            cwd_ = fs::canonical(new_path);
        } 
        else {
            std::cerr << "No such directory\n";
        }
    }

    void copy(const fs::path& src, const fs::path& dst) 
    {
        std::error_code ec;
        fs::copy(src, dst, fs::copy_options::recursive, ec);
        if (ec) std::cerr << "Copy failed: " << ec.message() << "\n";
    }

    void remove(const fs::path& p) 
    {
        std::error_code ec;
        fs::remove_all(p, ec);
        if (ec) std::cerr << "Delete failed: " << ec.message() << "\n";
    }

    void rename(const fs::path& from, const fs::path& to) 
    {
        std::error_code ec;
        fs::rename(from, to, ec);
        if (ec) std::cerr << "Rename failed: " << ec.message() << "\n";
    }

private:
    fs::path cwd_;
};

int main() 
{
    FileManager fm(fs::current_path());
    std::string cmd, arg1, arg2;
  
    while (true) 
    {
        std::cout << "> ";
        std::cin >> cmd;
        if (cmd == "ls") fm.ls();
          
        else if (cmd == "cd") 
        { 
          std::cin >> arg1; 
          fm.cd(arg1); 
        }
        else if (cmd == "cp") 
        { 
          std::cin >> arg1 >> arg2; 
          fm.copy(arg1, arg2); 
        }
        else if (cmd == "rm") 
        { 
          std::cin >> arg1; 
          fm.remove(arg1); 
        }
          
        else if (cmd == "exit") break;
    }
}
