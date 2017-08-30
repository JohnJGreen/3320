
/*
john green
cse 3320
assignment 3 b
*/

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <string.h>
#include <time.h>
#include <sys/ipc.h>
#include <sys/shm.h>

struct entry  {
	char string[255];
	float mag;
};

struct share  {
    int flags[100];
    struct entry ls[32000];
};
	
int main(int argc, char *argv[])
{	
	int numberOfChildren=0, i, j, shmid, count;
	int ch=0, lines=0, begin, end, tracker;
	int stillWaiting;
	float time, max;
	char cmd[255], *tok;
	struct share *shm;
	key_t key=1111;
	clock_t start, diff;
	pid_t *childPids = NULL;
	pid_t p;
	FILE *fp;
	
	do{
		printf("how many children? ");
		fgets (cmd, sizeof(cmd), stdin);
		numberOfChildren = atoi(cmd);
	} while ( numberOfChildren <= 0);
	
	childPids = malloc(numberOfChildren * sizeof(pid_t));
	
	fp = fopen("all_month.csv", "r");
    if (fp == NULL){
		printf("all_month.csv failed\n");
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

	start = clock();

	childPids = malloc(numberOfChildren * sizeof(pid_t));

    if ((shmid = shmget(key, sizeof(struct share), IPC_CREAT | 0666)) < 0) {
        perror("shmget");
        exit(1);
    }
    if ((shm = shmat(shmid, NULL, 0)) == (struct share *) -1) {
        perror("shmat");
        exit(1);
    }

	fp = fopen("all_month.csv", "r");

	for (i = 0; i < numberOfChildren; i++) {
        shm->flags[i] = -1;
    }
	count=0;
    while ((fgets (cmd, sizeof(cmd), fp)) != NULL) {
		strcpy(shm->ls[count].string, cmd);
		if(shm->ls[count].string[0] == 't'){
            shm->ls[count].mag = 99;
		}
		else{
            tok = strtok(cmd, ",");
            tok = strtok(NULL, ",");
            tok = strtok(NULL, ",");
            tok = strtok(NULL, ",");
            tok = strtok(NULL, ",");
            shm->ls[count].mag = atof(tok);
		}
		count++;
    }
    fclose(fp);
	
	fp = fopen("output.csv", "w+");
	while (1) {
		for (i = 0; i < numberOfChildren; i++) {
			if ((p = fork()) == 0) {
				if ((shmid = shmget(key, sizeof(struct share), 0666)) < 0) {
					perror("shmget");
					exit(1);
				}
				if ((shm = shmat(shmid, NULL, 0)) == (struct share *) -1) {
					perror("shmat");
					exit(1);
				}
				begin = i*(lines/numberOfChildren);
				if (i == numberOfChildren-1){
					end = lines+1;
				}
				else{
					end = i*(lines/numberOfChildren)+(lines/numberOfChildren);
				}
				max = -99.0;
				for(j=begin; j<=end; j++){
					if (shm->ls[j].mag > max){
		                max = shm->ls[j].mag;
		                tracker = j;
		            }
				}
				shm->flags[i] = tracker;
				shmdt(shm);
				_exit(0);
			}
			else {
		      childPids[i] = p;
			}
		}
		
		do {
		   stillWaiting = 0;
		    for (i = 0; i < numberOfChildren; i++) {
		       if (childPids[i] > 0) {
		          if (waitpid(childPids[i], NULL, WNOHANG) != 0) {
		             childPids[i] = 0;
		          }
		          else {
		             stillWaiting = 1;
		          }
		       }
		    }
		} while (stillWaiting);
		
		max = -99.0;
		for (i = 0; i < numberOfChildren; i++) {
			if (shm->ls[shm->flags[i]].mag > max){
				max = shm->ls[shm->flags[i]].mag;
				tracker = i;
			}
		}
		if(max == -50.0){
			break;
		}
		strcpy(cmd, shm->ls[shm->flags[tracker]].string);
		fputs(cmd,fp);
		shm->ls[shm->flags[tracker]].mag = -50.0;
		shm->flags[tracker] = -1;
	}
	shmdt(shm);
	fclose(fp);
	
	
	
	diff = clock()-start;
	time = ((float) diff)/ CLOCKS_PER_SEC;
	printf("time taken for %d processes = %.2f sec.\n", numberOfChildren, time);
	return 0;
}
