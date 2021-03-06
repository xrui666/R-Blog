package com.xr.web.admin;

import com.xr.pojo.Blog;
import com.xr.pojo.User;
import com.xr.service.BlogService;
import com.xr.service.TagService;
import com.xr.service.TypeService;
import com.xr.util.UploadUtils;
import com.xr.vo.BlogQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;

/**
 * @author : xr
 * @create : 2020-02-08
 * @describe:
 */
@Controller
@RequestMapping("/admin")
public class BlogController {

    private static final String INPUT="admin/blogs-input";
    private static final String LIST="admin/blogs";
    private static final String REDIRECT_LIST="redirect:/admin/blogs";

    @Autowired
    private BlogService blogService;

    @Autowired
    private TypeService typeService;

    @Autowired
    private TagService tagService;

    @Value("${upload.path}")
    private String uploadPath;
    @Value("${picture.path}")
    private String picturePath;


    @GetMapping("/blogs")
    public String blogs(@PageableDefault(size = 5,sort = {"updateTime"},direction = Sort.Direction.DESC) Pageable pageable, BlogQuery blog, Model model){
        model.addAttribute("types",typeService.listType());
        model.addAttribute("page",blogService.listBlog(pageable,blog));
        return LIST;
    }

    @PostMapping("/blogs/search")
    public String search(@PageableDefault(size = 5,sort = {"updateTime"},direction = Sort.Direction.DESC) Pageable pageable, BlogQuery blog, Model model){
        model.addAttribute("page",blogService.listBlog(pageable,blog));
        return "admin/blogs ::blogList"; //返回admin/blogs页面下的一个片段blogList,实现局部刷新
    }


    @GetMapping("/blogs/input")
    public String input(Model model){
        setTypeAndTag(model);
        model.addAttribute("blog",new Blog());
        return INPUT;
    }

    public void setTypeAndTag(Model model){
        model.addAttribute("types",typeService.listType()); //获取所有分类
        model.addAttribute("tags",tagService.listTag());  //获取所有标签
    }

    @GetMapping("/blogs/{id}/input")
    public String editInput(@PathVariable Long id, Model model){
        setTypeAndTag(model);
        Blog blog=blogService.getBlog(id);
        blog.init(); //初始化，将标签数组转换为字符串
        model.addAttribute("blog",blog);
        return INPUT;
    }

    @PostMapping("/blogs")
    public String post(@RequestParam("file") MultipartFile file, Blog blog, RedirectAttributes attributes, HttpSession session){
        // 图片路径
        String imgUrl = null;
        try {
            //上传
            String filename = UploadUtils.upload(file, uploadPath, file.getOriginalFilename());
            if (!filename.isEmpty()) {
                imgUrl = new File(picturePath) + "/" + filename;
                blog.setUser((User) session.getAttribute("user"));
                blog.setType(typeService.getType(blog.getType().getId()));
                blog.setTags(tagService.listTag(blog.getTagIds()));
                blog.setFirstPicture(imgUrl);
                Blog b;
                if (blog.getId() == null) {
                    b =  blogService.saveBlog(blog); //id为空，执行新建操作
                } else {
                    b = blogService.updateBlog(blog.getId(), blog); //id有值，执行更新操作
                }
                if(b == null){
                    attributes.addFlashAttribute("message","操作失败");
                }else {
                    attributes.addFlashAttribute("message","操作成功");
                }
            } else {
                attributes.addFlashAttribute("message","上传首图出错，操作失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            attributes.addFlashAttribute("message","操作失败");
        }
        return REDIRECT_LIST;
    }


    @GetMapping("/blogs/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attributes){
        blogService.deleteBlog(id);
        attributes.addFlashAttribute("message","删除成功");
        return REDIRECT_LIST;
    }


}
