#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <string.h>
#include <time.h>

struct entry  {
	char *string;
	float mag;
};

void merge(int num, int size){
	
	int i, j, lines, count, ch=0, *ticker;
	int arrnum, *linecount;
	float max;
	char name[4], *tok, buff[255];
	FILE *fp;
	struct entry ***ls;
	
	ls = malloc(num*sizeof(struct entry*));
	ticker = malloc(num*sizeof(int));
	linecount = malloc(num*sizeof(int));
	//initialization
	for(i=0; i<num; i++){
		//track position in array
		ticker[i] = 0;
		// open file
		sprintf(name,"x%02d",i);
		fp = fopen(name, "r");
		if (fp == NULL){
			printf("%s failed\n", name);
		}
		lines = 0;
		count = 0;
		//get line count
		while(!feof(fp))
		{
			ch = fgetc(fp);
			if(ch == '\n')
			{
				lines++;
			}
		}
		fclose(fp);
		linecount[i] = lines;
		//reead in file
		fp = fopen(name, "r");
		ls[i] = malloc((lines+1) * sizeof(ls));
	    while ((fgets (buff, sizeof(buff), fp)) != NULL) {
			ls[i][count] = malloc(sizeof(struct entry*));
			ls[i][count]->string = malloc(strlen(buff)+1);
			strcpy(ls[i][count]->string, buff);
	        tok = strtok(buff, ",");
			tok = strtok(NULL, ",");
			tok = strtok(NULL, ",");
			tok = strtok(NULL, ",");
			tok = strtok(NULL, ",");
			ls[i][count]->mag = atof(tok);
			count++;
	    }
	    fclose(fp);
	}
	//keep header at the top
	for(i=0; i<num; i++){
		if(ls[i][0]->string[0] == 't'){
			ls[i][0]->mag = 99;
		}
	}
	// merge to file
	fp = fopen("output.csv", "w+");
	for(i=0; i<size; i++){
		max = -99;
		arrnum = 0;
		//loop through each array
		for(j=0; j<num; j++){
			//check each array in range and larger mag
			if((linecount[j] > ticker[j]) && (ls[j][ticker[j]]->mag > max)){
				arrnum = j;
				max = ls[j][ticker[j]]->mag;
			}
		}
		fputs(ls[arrnum][ticker[arrnum]]->string,fp);
		ticker[arrnum] += 1;
	}
	fclose(fp);
}
	
int main(int argc, char *argv[])
{	
	int numberOfChildren=0, i;
	int ch=0, lines=0;
	float time;
	char cmd[255];
	char *args[] = {" ",0};
	clock_t start, diff;
	pid_t *childPids = NULL;
	pid_t p;
	FILE *fp;
	
	do{
		printf("how many children? ");
		fgets (cmd, sizeof(cmd), stdin);
		numberOfChildren = atoi(cmd);
	} while ( numberOfChildren <= 0);
	
	// Allocate array of child PIDs
	childPids = malloc(numberOfChildren * sizeof(pid_t));
	// get line count
	fp = fopen("all_month.csv", "r");
    if (fp == NULL){
		printf("%s failed\n", argv[0]);
		return -1;
	}
         
	while(!feof(fp))
	{
		ch = fgetc(fp);
		if(ch == '\n')
		{
			lines++;
		}
	}
	fclose(fp);
	// split files
	sprintf(cmd, "split -d -l %d all_month.csv", lines/numberOfChildren+1);
	system(cmd);
	
	start = clock();
	
	// Start up children 
	for (i = 0; i < numberOfChildren; i++) {
	   if ((p = fork()) == 0) {
	      sprintf(cmd, "x%02d", i);
	      args[0] = cmd;
	      execv("sort.out", args);
	      exit(0);
	   }
	   else {
	      childPids[i] = p;
	   }
	}
	
	// Wait for children to exit
	int stillWaiting;
	do {
	   stillWaiting = 0;
	    for (i = 0; i < numberOfChildren; i++) {
	       if (childPids[i] > 0) {
	          if (waitpid(childPids[i], NULL, WNOHANG) != 0) {
	             // Child is done
	             childPids[i] = 0;
	          }
	          else {
	             // Still waiting on this child
	             stillWaiting = 1;
	          }
	       }
	       sleep(0);
	    }
	} while (stillWaiting);
	
	merge(numberOfChildren, lines);
	//remove temp files
	for(i=0; i<numberOfChildren; i++){
		sprintf(cmd, "rm x%02d",i);
		system(cmd);
	}
	diff = clock()-start;
	time = ((float) diff)/ CLOCKS_PER_SEC;
	printf("time taken for %d processes = %.2f sec.\n", numberOfChildren, time);
	return 0;
}
