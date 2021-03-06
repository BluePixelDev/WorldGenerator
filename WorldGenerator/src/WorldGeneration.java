import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;

import Utility.BiomeType;
import Utility.Tile;
import Utility.TileType;
import Utility.Vector2;

public class WorldGeneration 
{
	public static long worldSeed = 0;	
	public static int maxWorldHeight = 64;
	
	//---- COLOR SETTINGS ----
	public static Color grassColor = new Color(92, 166, 31);	
	public static Color sandColor = new Color(218, 207, 143);
	
	public static Color stoneColor = new Color(120, 120, 120);
	public static Color snowColor = Color.white;
	
	public static Color waterColor = new Color(49, 109, 195);
	
	public static Color treeColor = new Color(0, 255, 26);
	
	public static float waterLevel = 0f;
	public static float sandReach = .04f;
	public static float snowLevel = .45f;
	public static float stoneLevel = .35f;
	
	//---- OTHER -----
	public static double forestDensity = .95d;
	public static boolean randomColors = false; 
	
	//---- COLORING ----		
	public static Random rn = new Random();
	public static Image worldImage;
	private static BufferedImage wMap;
	
	public static OpenSimplexNoise noise = new OpenSimplexNoise();	
	private static ArrayList<Vector2> treePositions = new ArrayList<Vector2>();
		
	//---- MAIN ----
	public static void GenerateWorld(boolean init) 
	{	
		treePositions.clear();
		
		if(init) Main.grid = new Tile[Main.simulationSize][Main.simulationSize];
		wMap = new BufferedImage(Main.simulationSize, Main.simulationSize,BufferedImage.TYPE_INT_RGB);
		
		worldSeed = GenerateSeed(worldSeed);
		if (randomColors) GenerateColors();
		
		for(int x = 0; x < Main.simulationSize; x++) 
		{
			for(int y = 0; y < Main.simulationSize; y++) 
			{
				Tile t = new Tile();
				t.setPosition(new Vector2(x, y));
				
				GenerateNoise(t);
				ProcessMaps(t);
				
				//Adds tile to buffered image
				wMap.setRGB(x, y, t.getColor().getRGB());
				
				Main.grid[x][y] = t;	
			}
		}
		
		AddDetails();
		worldImage = wMap;
	}	
	
	private static void GenerateColors() 
	{
		grassColor = GenerateRandomColor();
		sandColor = GenerateRandomColor();
		stoneColor = GenerateRandomColor();
		snowColor = GenerateRandomColor();
		treeColor = GenerateRandomColor();
	}
	
	//Sets seed for world generation
	private static long GenerateSeed(long seed) 
	{
		if(seed != 0) 
		{
			rn.setSeed(seed);
			noise = new OpenSimplexNoise(seed);
		}
		else 
		{
			seed = System.currentTimeMillis();
			rn.setSeed(seed);
			noise = new OpenSimplexNoise(seed);
		}
		
		return seed;
	}
	
	//Handles world shape generation
	private static void GenerateNoise(Tile t) 
	{
		int x = t.getPosition().getX();
		int y = t.getPosition().getY();
		
		//---- NOISE GENERATION ----
		float heightStep =  1 / (float)maxWorldHeight;
		float finalHeight = heightStep * (Math.round(NoiseManager.GetTile(x, y) / heightStep));
		
		t.setHeight(finalHeight);
	}
	
	//Processes noise maps to create world
	private static void ProcessMaps(Tile t) 
	{
		int x = t.getPosition().getX();
		int y = t.getPosition().getY();
				
		Tile tUp = null; //Tile above current tile
		if(y != 0) tUp = Main.grid[t.getPosition().getX()][t.getPosition().getY()-1];
		
		Color tileColor = waterColor;
		
		//---- WATER ----
		if(t.getHeight() < waterLevel) 
		{														
			t.setType(TileType.Water);	
			t.setBiome(BiomeType.Sea);	
			
		}
		else 
		{					
			tileColor = grassColor;
			t.setType(TileType.Grass);
			t.setBiome(BiomeType.Plains);
			
			//---- BEACHES ----
			if(Math.abs(t.getHeight() - waterLevel) < sandReach ) 
			{									
				t.setType(TileType.Sand);		
				tileColor = sandColor;		
				t.setBiome(BiomeType.Beach);
			}
			
			//---- STONE ----
			if(t.getHeight() >= stoneLevel) 
			{		
				t.setType(TileType.Stone);					
				tileColor = stoneColor;
			}		
			
			//---- SNOW ----
			if(t.getHeight() >= snowLevel) 
			{
				t.setType(TileType.Snow);
				tileColor = snowColor;
			}
			
		}			
		
		if(tUp != null) 
		{
			//---- HEIGHT COLORING ----
			if(tUp.getHeight() < t.getHeight()) 
			{
				tileColor = MultiplyColorBrightness(tileColor, 1.1f);
			}
			
			if(tUp.getHeight() > t.getHeight()) 
			{
				tileColor = MultiplyColorBrightness(tileColor, .8f);
			}
		}
		
		if(t.getType() == TileType.Grass) 
		{
			double chance = rn.nextDouble();
			
			if(chance > forestDensity) 
			{
				Vector2 lastPos = new Vector2(0, 0);
				
				if(treePositions.size() > 0)
					lastPos = treePositions.get(treePositions.size() - 1);
				
				int xDis = Math.abs(t.getPosition().getX() - lastPos.getX());
				int yDis = Math.abs(t.getPosition().getY() - lastPos.getY());				
				
				if(yDis > 5 || xDis > 5) 
				{
					treePositions.add(t.getPosition());	
				}			
			}
				
		}
					
		//Sets tile color and assigns value to the grid
		t.setColor(tileColor);				
	}
	
	//Adds details to map such as trees
	private static void AddDetails() 
	{
		
		BufferedImage treeImage = null;
		
		//Loads the tree image
		try 
		{			
			URL resource = Main.class.getResource("resources/tree.png");		
		    treeImage = ImageIO.read(resource);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		if(treeImage == null) return;
		
		for(Vector2 v : treePositions)
		{
			int resX = v.getX();
			int resY = v.getY();
			
			Tile t = Main.grid[resX][resY];
			
			if(t.getType() != TileType.Grass) continue;
			
			for(int x = 0; x < treeImage.getWidth(); x++) 
			{
				for(int y = 0; y < treeImage.getHeight(); y++) 
				{
					int imX = x + resX - treeImage.getWidth() / 2;
					int imY = y + resY - treeImage.getHeight() / 2;
					
					if(imX > Main.simulationSize - 1 || imX < 0) continue;
					if(imY > Main.simulationSize - 1 || imY < 0) continue;		
					
					Color color = new Color(treeImage.getRGB(x, y), true);
					
					if(color.getAlpha() != 0)
					wMap.setRGB(imX, imY, MultiplyByGrayscale(color, treeColor).getRGB());
				}
			}
		}
	}
	
	//---- METHODS ----	
	//Generates random color
	private static Color GenerateRandomColor() 
	{
		return new Color(rn.nextInt(256), rn.nextInt(256), rn.nextInt(256));
	}
	
	//Multiplies color by grayscale image
	private static Color MultiplyByGrayscale(Color ca, Color cb) 
	{	
		int r = Clamp((int)((float)ca.getRed() / (float)255 * cb.getRed()), 0, 255);
		int g = Clamp((int)((float)ca.getGreen() / (float)255 * cb.getGreen()), 0, 255);
		int b = Clamp((int)((float)ca.getBlue() / (float)255 * cb.getBlue()), 0, 255);
		
		ca = new Color(r, g, b);
		
		return ca;
	}
	
	//Multiplies color by float to change brightness
	private static Color MultiplyColorBrightness(Color c, float br) 
	{	
		int r = Clamp((int)(c.getRed() * br), 0, 255);
		int g = Clamp((int)(c.getGreen() * br), 0, 255);
		int b = Clamp((int)(c.getBlue() * br), 0, 255);
		
		c = new Color(r, g, b);
		
		return c;
	}
	
	//---- CLAMPING ----
	//Clamps value between minimal and maximal value
	public static double Clamp(double a, double min, double max) 
	{
		if(a < min) a = min;
		if(a > max) a = max;
			
		return a;
	}	
	public static int Clamp(int a, int min, int max) 
	{
		if(a < min) a = min;
		if(a > max) a = max;
			
		return a;
	}	

	//---- LERPING ----
	//Linear interpolation, used for smoothing noise maps
	public static float Lerp(float a, float b, float f)
	{
		return a + f * (b - a);
	}
	public static double Lerp(double a, double b, double f)
	{
		return a + f * (b - a);
	}	
}
