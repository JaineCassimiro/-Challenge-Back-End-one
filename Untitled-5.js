import type { Express } from "express";
import express from "express";
import { createServer, type Server } from "http";
import { storage } from "./storage";
import { setupAuth, isAuthenticated } from "./replitAuth";
import path from "path";
import { insertTopicSchema, updateTopicSchema, insertReplySchema } from "@shared/schema";
import { fromZodError } from "zod-validation-error";

export async function registerRoutes(app: Express): Promise<Server> {
  // Serve static HTML site
  app.use('/public', express.static(path.join(__dirname, '../public')));
  
  // Route for HTML landing page
  app.get('/site', (req, res) => {
    res.sendFile(path.join(__dirname, '../public/index.html'));
  });
  
  // Auth middleware
  await setupAuth(app);

  // Auth routes
  app.get('/api/auth/user', isAuthenticated, async (req: any, res) => {
    try {
      const userId = req.user.claims.sub;
      const user = await storage.getUser(userId);
      res.json(user);
    } catch (error) {
      console.error("Error fetching user:", error);
      res.status(500).json({ message: "Failed to fetch user" });
    }
  });

  // Users endpoint  
  app.get('/api/users', isAuthenticated, async (req, res) => {
    try {
      // For now, return mock data. In a real app, this would come from the database
      const users = [
        {
          id: "1",
          email: "jaine.silva@email.com",
          firstName: "Jaine",
          lastName: "Silva",
          profileImageUrl: "https://images.unsplash.com/photo-1494790108755-2616b612b123?ixlib=rb-4.0.3&auto=format&fit=crop&w=150&h=150",
          createdAt: new Date("2024-01-15"),
          updatedAt: new Date("2024-01-15")
        },
        {
          id: "2", 
          email: "maria.santos@email.com",
          firstName: "Maria",
          lastName: "Santos",
          profileImageUrl: "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?ixlib=rb-4.0.3&auto=format&fit=crop&w=150&h=150",
          createdAt: new Date("2024-02-01"),
          updatedAt: new Date("2024-02-01")
        },
        {
          id: "3",
          email: "joao.developer@email.com", 
          firstName: "João",
          lastName: "Developer",
          profileImageUrl: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?ixlib=rb-4.0.3&auto=format&fit=crop&w=150&h=150",
          createdAt: new Date("2024-01-20"),
          updatedAt: new Date("2024-01-20")
        }
      ];
      res.json(users);
    } catch (error) {
      console.error("Error fetching users:", error);
      res.status(500).json({ message: "Failed to fetch users" });
    }
  });

  // Statistics endpoint
  app.get('/api/stats', isAuthenticated, async (req, res) => {
    try {
      const [topicCount, userCount, todayTopics, totalReplies] = await Promise.all([
        storage.getTopicCount(),
        storage.getUserCount(),
        storage.getTodayTopicCount(),
        storage.getTotalRepliesCount(),
      ]);

      res.json({
        totalTopics: topicCount,
        totalUsers: userCount,
        todayTopics,
        totalReplies,
        onlineUsers: Math.floor(Math.random() * 200) + 50, // Simulated online users
        apiRequests: Math.floor(Math.random() * 20000) + 10000, // Simulated API requests
      });
    } catch (error) {
      console.error("Error fetching stats:", error);
      res.status(500).json({ message: "Failed to fetch statistics" });
    }
  });

  // Topic CRUD operations
  app.get('/api/topicos', isAuthenticated, async (req, res) => {
    try {
      const topics = await storage.getAllTopics();
      res.json(topics);
    } catch (error) {
      console.error("Error fetching topics:", error);
      res.status(500).json({ message: "Failed to fetch topics" });
    }
  });

  app.get('/api/topicos/:id', isAuthenticated, async (req, res) => {
    try {
      const id = parseInt(req.params.id);
      if (isNaN(id)) {
        return res.status(400).json({ message: "Invalid topic ID" });
      }

      const topic = await storage.getTopicById(id);
      if (!topic) {
        return res.status(404).json({ message: "Topic not found" });
      }

      res.json(topic);
    } catch (error) {
      console.error("Error fetching topic:", error);
      res.status(500).json({ message: "Failed to fetch topic" });
    }
  });

  app.post('/api/topicos', isAuthenticated, async (req: any, res) => {
    try {
      const userId = req.user.claims.sub;
      const topicData = { ...req.body, authorId: userId };
      
      const validatedData = insertTopicSchema.parse(topicData);
      const topic = await storage.createTopic(validatedData);
      
      res.status(201).json(topic);
    } catch (error: any) {
      console.error("Error creating topic:", error);
      if (error.name === 'ZodError') {
        const validationError = fromZodError(error);
        return res.status(400).json({ message: validationError.message });
      }
      res.status(500).json({ message: "Failed to create topic" });
    }
  });

  app.put('/api/topicos/:id', isAuthenticated, async (req: any, res) => {
    try {
      const id = parseInt(req.params.id);
      if (isNaN(id)) {
        return res.status(400).json({ message: "Invalid topic ID" });
      }

      const userId = req.user.claims.sub;
      const existingTopic = await storage.getTopicById(id);
      
      if (!existingTopic) {
        return res.status(404).json({ message: "Topic not found" });
      }

      if (existingTopic.authorId !== userId) {
        return res.status(403).json({ message: "You can only edit your own topics" });
      }

      const validatedData = updateTopicSchema.parse(req.body);
      const updatedTopic = await storage.updateTopic(id, validatedData);
      
      res.json(updatedTopic);
    } catch (error: any) {
      console.error("Error updating topic:", error);
      if (error.name === 'ZodError') {
        const validationError = fromZodError(error);
        return res.status(400).json({ message: validationError.message });
      }
      res.status(500).json({ message: "Failed to update topic" });
    }
  });

  app.delete('/api/topicos/:id', isAuthenticated, async (req: any, res) => {
    try {
      const id = parseInt(req.params.id);
      if (isNaN(id)) {
        return res.status(400).json({ message: "Invalid topic ID" });
      }

      const userId = req.user.claims.sub;
      const existingTopic = await storage.getTopicById(id);
      
      if (!existingTopic) {
        return res.status(404).json({ message: "Topic not found" });
      }

      if (existingTopic.authorId !== userId) {
        return res.status(403).json({ message: "You can only delete your own topics" });
      }

      const deleted = await storage.deleteTopic(id);
      if (deleted) {
        res.status(204).send();
      } else {
        res.status(404).json({ message: "Topic not found" });
      }
    } catch (error) {
      console.error("Error deleting topic:", error);
      res.status(500).json({ message: "Failed to delete topic" });
    }
  });

  // Reply operations
  app.get('/api/topicos/:id/replies', isAuthenticated, async (req, res) => {
    try {
      const topicId = parseInt(req.params.id);
      if (isNaN(topicId)) {
        return res.status(400).json({ message: "Invalid topic ID" });
      }

      const replies = await storage.getRepliesByTopicId(topicId);
      res.json(replies);
    } catch (error) {
      console.error("Error fetching replies:", error);
      res.status(500).json({ message: "Failed to fetch replies" });
    }
  });

  app.post('/api/topicos/:id/replies', isAuthenticated, async (req: any, res) => {
    try {
      const topicId = parseInt(req.params.id);
      if (isNaN(topicId)) {
        return res.status(400).json({ message: "Invalid topic ID" });
      }

      const userId = req.user.claims.sub;
      const replyData = { ...req.body, topicId, authorId: userId };
      
      const validatedData = insertReplySchema.parse(replyData);
      const reply = await storage.createReply(validatedData);
      
      res.status(201).json(reply);
    } catch (error: any) {
      console.error("Error creating reply:", error);
      if (error.name === 'ZodError') {
        const validationError = fromZodError(error);
        return res.status(400).json({ message: validationError.message });
      }
      res.status(500).json({ message: "Failed to create reply" });
    }
  });

  const httpServer = createServer(app);
  return httpServer;
}
